package com.hit.websocket.client.observability.micrometer;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.connection.ConnectionState;
import com.hit.websocket.client.observability.WebSocketObserver;
import com.hit.websocket.client.observability.model.ConnectionEvent;
import com.hit.websocket.client.observability.model.ConnectionEventDetails;
import com.hit.websocket.client.observability.model.ConnectionEventType;
import com.hit.websocket.client.transport.CloseReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Low-cardinality Micrometer metrics for WebSocket client connections. */
public final class MicrometerWebSocketObserver implements WebSocketObserver {

    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final Map<ConnectionId, AtomicInteger> stateGauges = new ConcurrentHashMap<>();
    private final Map<ConnectionId, AtomicLong> lastMessageEpochSeconds = new ConcurrentHashMap<>();
    private final Map<ConnectionId, StateTiming> stateTimings = new ConcurrentHashMap<>();
    private final Map<ConnectionId, AttemptTiming> attemptTimings = new ConcurrentHashMap<>();
    private final Map<ConnectionId, ConnectionEvent> latestEvents = new ConcurrentHashMap<>();

    public MicrometerWebSocketObserver(MeterRegistry meterRegistry) {
        this(meterRegistry, Clock.systemDefaultZone());
    }

    public MicrometerWebSocketObserver(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Override
    public void onEvent(ConnectionEvent event) {
        latestEvents.compute(event.connectionId(), (connectionId, previous) -> {
            if (previous != null && (event.generation() < previous.generation()
                    || event.generation() == previous.generation() && event.sequence() <= previous.sequence())) {
                return previous;
            }
            recordEvent(event);
            return event;
        });
    }

    private void recordEvent(ConnectionEvent event) {
        stateGauge(event.connectionId()).set(metricValue(event.state()));
        recordStateDuration(event);
        recordLifecycleMetrics(event);
        Counter.builder("websocket.client.events")
                .description("WebSocket client lifecycle events")
                .tag("source", event.connectionId().provider())
                .tag("connection", event.connectionId().name())
                .tag("event", event.type().name().toLowerCase())
                .register(meterRegistry)
                .increment();

        if (event.type() == ConnectionEventType.ERROR) {
            ConnectionEventDetails.Failure failure = event.details() instanceof ConnectionEventDetails.Failure value
                    ? value : null;
            Counter.builder("websocket.client.errors")
                    .description("WebSocket client errors")
                    .tag("source", event.connectionId().provider())
                    .tag("connection", event.connectionId().name())
                    .tag("category", failure == null ? "unknown" : failure.category().name().toLowerCase())
                    .register(meterRegistry)
                    .increment();
        }
    }

    @Override
    public void onMessageReceived(ConnectionId connectionId, long generation, int payloadBytes) {
        if (isStaleGeneration(connectionId, generation)) return;
        lastMessageGauge(connectionId).set(clock.instant().getEpochSecond());
        meterRegistry.counter("websocket.client.messages.received",
                "source", connectionId.provider(),
                "connection", connectionId.name()).increment();
        meterRegistry.counter("websocket.client.bytes.received",
                "source", connectionId.provider(),
                "connection", connectionId.name()).increment(Math.max(payloadBytes, 0));
    }

    @Override
    public void onMessageSent(ConnectionId connectionId, long generation, int payloadBytes) {
        if (isStaleGeneration(connectionId, generation)) return;
        meterRegistry.counter("websocket.client.messages.sent",
                "source", connectionId.provider(),
                "connection", connectionId.name()).increment();
        meterRegistry.counter("websocket.client.bytes.sent",
                "source", connectionId.provider(),
                "connection", connectionId.name()).increment(Math.max(payloadBytes, 0));
    }

    private AtomicInteger stateGauge(ConnectionId connectionId) {
        return stateGauges.computeIfAbsent(connectionId, id -> {
            AtomicInteger value = new AtomicInteger(metricValue(ConnectionState.STOPPED));
            Gauge.builder("websocket.client.state", value, AtomicInteger::get)
                    .description("Current WebSocket client connection state code")
                    .tag("source", id.provider())
                    .tag("connection", id.name())
                    .register(meterRegistry);
            return value;
        });
    }

    private boolean isStaleGeneration(ConnectionId connectionId, long generation) {
        ConnectionEvent event = latestEvents.get(connectionId);
        return event == null || event.generation() != generation;
    }

    private AtomicLong lastMessageGauge(ConnectionId connectionId) {
        return lastMessageEpochSeconds.computeIfAbsent(connectionId, id -> {
            AtomicLong value = new AtomicLong();
            Gauge.builder("websocket.client.last.message.epoch.seconds", value, AtomicLong::get)
                    .description("Epoch second of the last received WebSocket frame")
                    .tag("source", id.provider())
                    .tag("connection", id.name())
                    .register(meterRegistry);
            return value;
        });
    }

    private void recordStateDuration(ConnectionEvent event) {
        stateTimings.compute(event.connectionId(), (id, previous) -> {
            if (previous != null && previous.state() == event.state()) return previous;
            if (previous != null && event.occurredAt().isAfter(previous.enteredAt())) {
                Timer.builder("websocket.client.state.duration")
                        .description("Time spent in a WebSocket lifecycle state")
                        .tag("source", id.provider())
                        .tag("connection", id.name())
                        .tag("state", previous.state().name().toLowerCase())
                        .register(meterRegistry)
                        .record(Duration.between(previous.enteredAt(), event.occurredAt()));
            }
            return new StateTiming(event.state(), event.occurredAt());
        });
    }

    private void recordLifecycleMetrics(ConnectionEvent event) {
        String metric = switch (event.type()) {
            case CONNECT_REQUESTED -> "websocket.client.connections.attempted";
            case TRANSPORT_CONNECTED -> "websocket.client.connections.transport";
            case DISCONNECT_REQUESTED -> "websocket.client.disconnects";
            case RETRY_SCHEDULED -> "websocket.client.retries";
            case HEARTBEAT_TIMEOUT -> "websocket.client.heartbeat.timeouts";
            default -> null;
        };
        if (metric != null) {
            meterRegistry.counter(metric, "source", event.connectionId().provider(),
                    "connection", event.connectionId().name()).increment();
        }
        boolean terminalFailure = event.type() == ConnectionEventType.ERROR
                && event.details() instanceof ConnectionEventDetails.Failure failure
                && failure.terminal()
                && event.state() != ConnectionState.CONNECTING;
        boolean unexpectedClose = event.type() == ConnectionEventType.TRANSPORT_CLOSED
                && event.details() instanceof ConnectionEventDetails.TransportClosed(CloseReason reason)
                && !reason.initiatedByClient();
        if (terminalFailure || unexpectedClose) {
            meterRegistry.counter("websocket.client.connection.losses",
                    "source", event.connectionId().provider(),
                    "connection", event.connectionId().name()).increment();
        }

        if (event.type() == ConnectionEventType.CONNECT_REQUESTED) {
            attemptTimings.put(event.connectionId(), new AttemptTiming(event.generation(), event.occurredAt()));
        } else if (event.type() == ConnectionEventType.TRANSPORT_CONNECTED) {
            AttemptTiming timing = attemptTimings.remove(event.connectionId());
            if (timing != null && timing.generation() == event.generation()) {
                recordTimer("websocket.client.connect.duration", event.connectionId(),
                        Duration.between(timing.startedAt(), event.occurredAt()));
            }
        } else if (event.type() == ConnectionEventType.FAILED || event.type() == ConnectionEventType.STOPPED) {
            attemptTimings.remove(event.connectionId());
        }
    }

    private void recordTimer(String name, ConnectionId id, Duration duration) {
        if (duration.isNegative()) return;
        Timer.builder(name)
                .tag("source", id.provider())
                .tag("connection", id.name())
                .register(meterRegistry)
                .record(duration);
    }

    private record StateTiming(ConnectionState state, Instant enteredAt) {
    }

    private record AttemptTiming(long generation, Instant startedAt) {
    }

    private static int metricValue(ConnectionState state) {
        return switch (state) {
            case STOPPED -> 0;
            case CONNECTING -> 1;
            case CONNECTED -> 2;
            case RETRY_WAIT -> 3;
            case DISCONNECTING -> 4;
            case FAILED -> 5;
        };
    }
}

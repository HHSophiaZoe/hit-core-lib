package com.hit.websocket.client.observability.memory;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.connection.ConnectionState;
import com.hit.websocket.client.observability.model.ConnectionDailySnapshot;
import com.hit.websocket.client.observability.model.ConnectionEvent;
import com.hit.websocket.client.observability.model.ConnectionEventDetails;
import com.hit.websocket.client.observability.model.ConnectionEventType;
import com.hit.websocket.client.observability.model.ConnectionSnapshot;
import com.hit.websocket.client.observability.ConnectionEventQuery;
import com.hit.websocket.client.observability.ConnectionEventSource;
import com.hit.websocket.client.observability.ConnectionObserver;
import com.hit.websocket.client.observability.ConnectionSnapshotQuery;
import com.hit.websocket.client.transport.CloseReason;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Current-state registry. Stale generations and out-of-order events are ignored. */
public final class InMemoryConnectionStateRegistry
        implements ConnectionObserver, ConnectionSnapshotQuery, ConnectionEventSource, ConnectionEventQuery {

    private static final int DEFAULT_RECENT_EVENT_CAPACITY = 0;

    private final ConcurrentMap<ConnectionId, MutableConnectionState> states = new ConcurrentHashMap<>();
    private final List<Consumer<ConnectionEvent>> subscribers = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<ConnectionId, RecentEventBuffer> recentEvents = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int recentEventCapacity;

    public InMemoryConnectionStateRegistry() {
        this(Clock.systemDefaultZone(), DEFAULT_RECENT_EVENT_CAPACITY);
    }

    public InMemoryConnectionStateRegistry(ZoneId statisticsZone) {
        this(Clock.system(statisticsZone), DEFAULT_RECENT_EVENT_CAPACITY);
    }

    public InMemoryConnectionStateRegistry(Clock clock, int recentEventCapacity) {
        this.clock = java.util.Objects.requireNonNull(clock);
        if (recentEventCapacity < 0) throw new IllegalArgumentException("recentEventCapacity cannot be negative");
        this.recentEventCapacity = recentEventCapacity;
    }

    @Override
    public void onEvent(ConnectionEvent event) {
        MutableConnectionState state = states.computeIfAbsent(event.connectionId(), id -> new MutableConnectionState(id, clock));
        if (!state.apply(event)) {
            return;
        }
        recordRecentEvent(event);
        for (Consumer<ConnectionEvent> subscriber : subscribers) {
            try {
                subscriber.accept(event);
            } catch (RuntimeException ignored) {
                // Observers are isolated from the connection lifecycle.
            }
        }
    }

    @Override
    public void onMessageReceived(ConnectionId connectionId, long generation, int payloadBytes) {
        states.computeIfAbsent(connectionId, id -> new MutableConnectionState(id, clock))
                .messageReceived(generation, payloadBytes);
    }

    @Override
    public void onMessageSent(ConnectionId connectionId, long generation, int payloadBytes) {
        states.computeIfAbsent(connectionId, id -> new MutableConnectionState(id, clock))
                .messageSent(generation, payloadBytes);
    }

    @Override
    public Optional<ConnectionSnapshot> find(ConnectionId connectionId) {
        MutableConnectionState state = states.get(connectionId);
        return state == null ? Optional.empty() : Optional.of(state.snapshot());
    }

    @Override
    public List<ConnectionSnapshot> findAll() {
        return states.values().stream()
                .map(MutableConnectionState::snapshot)
                .sorted(Comparator.comparing(snapshot -> snapshot.connectionId().value()))
                .toList();
    }

    @Override
    public Subscription subscribe(Consumer<ConnectionEvent> consumer) {
        subscribers.add(consumer);
        return () -> subscribers.remove(consumer);
    }

    @Override
    public List<ConnectionEvent> findRecent(ConnectionId connectionId, int limit) {
        if (recentEventCapacity == 0) return List.of();
        RecentEventBuffer events = recentEvents.get(connectionId);
        return events == null ? List.of() : events.snapshot(limit);
    }

    @Override
    public List<ConnectionEvent> findRecent(int limit) {
        if (recentEventCapacity == 0) return List.of();
        return recentEvents.values().stream()
                .flatMap(events -> events.snapshot(limit).stream())
                .sorted(Comparator.comparing(ConnectionEvent::occurredAt).reversed())
                .limit(normalizeLimit(limit))
                .toList();
    }

    private void recordRecentEvent(ConnectionEvent event) {
        if (recentEventCapacity == 0) return;
        recentEvents.computeIfAbsent(event.connectionId(), ignored -> new RecentEventBuffer(recentEventCapacity))
                .add(event);
    }

    private int normalizeLimit(int limit) {
        return Math.clamp(limit, 1, recentEventCapacity);
    }

    private static final class MutableConnectionState {
        private final ConnectionId connectionId;
        private final Clock clock;
        private final ZoneId statisticsZone;
        private long generation;
        private long lastSequence;
        private ConnectionState state = ConnectionState.STOPPED;
        private Instant stateChangedAt;
        private Instant connectedAt;
        private Instant readyAt;
        private Instant lastMessageAt;
        private Instant lastErrorAt;
        private String lastErrorCategory;
        private String lastErrorCode;
        private String lastErrorMessage;
        private long reconnectAttempts;
        private long receivedMessages;
        private long receivedBytes;
        private long sentMessages;
        private long sentBytes;
        private final DailyConnectionCounters dailyCounters;

        private MutableConnectionState(ConnectionId connectionId, Clock clock) {
            this.connectionId = connectionId;
            this.clock = clock;
            this.statisticsZone = clock.getZone();
            this.stateChangedAt = clock.instant();
            this.dailyCounters = new DailyConnectionCounters(LocalDate.now(clock));
        }

        private synchronized boolean apply(ConnectionEvent event) {
            if (event.generation() < generation
                    || (event.generation() == generation && event.sequence() <= lastSequence)) {
                return false;
            }
            if (event.generation() > generation) {
                generation = event.generation();
                lastSequence = 0;
                connectedAt = null;
                readyAt = null;
            }
            lastSequence = event.sequence();
            if (state != event.state()) {
                state = event.state();
                stateChangedAt = event.occurredAt();
            }
            dailyCounters.resetIfNeeded(dateOf(event.occurredAt()));

            if (event.type() == ConnectionEventType.CONNECT_REQUESTED) {
                dailyCounters.connectAttempted();
            } else if (event.type() == ConnectionEventType.TRANSPORT_CONNECTED) {
                connectedAt = event.occurredAt();
                dailyCounters.transportConnected();
            } else if (event.type() == ConnectionEventType.READY) {
                readyAt = event.occurredAt();
                reconnectAttempts = 0;
                dailyCounters.becameReady();
            } else if (event.type() == ConnectionEventType.RETRY_SCHEDULED) {
                if (event.details() instanceof ConnectionEventDetails.RetryScheduled retry) {
                    reconnectAttempts = retry.attempt();
                } else {
                    reconnectAttempts++;
                }
                dailyCounters.retryScheduled();
            } else if (event.type() == ConnectionEventType.ERROR
                    || event.type() == ConnectionEventType.HEARTBEAT_TIMEOUT) {
                lastErrorAt = event.occurredAt();
                ConnectionEventDetails.Failure failure = event.details() instanceof ConnectionEventDetails.Failure value
                        ? value : null;
                lastErrorCategory = failure == null ? "unknown" : failure.category().name().toLowerCase();
                lastErrorCode = failure == null ? null : failure.code();
                lastErrorMessage = failure == null ? null : failure.message();
                if (event.type() == ConnectionEventType.ERROR) {
                    dailyCounters.errorOccurred();
                    if (failure != null && failure.terminal() && state != ConnectionState.CONNECTING) {
                        dailyCounters.connectionLost();
                    }
                } else {
                    dailyCounters.heartbeatTimedOut();
                }
            } else if (event.type() == ConnectionEventType.DISCONNECT_REQUESTED) {
                dailyCounters.disconnectRequested();
            } else if (event.type() == ConnectionEventType.TRANSPORT_CLOSED) {
                if (event.details() instanceof ConnectionEventDetails.TransportClosed(CloseReason reason)
                        && !reason.initiatedByClient()) {
                    dailyCounters.connectionLost();
                }
            }
            return true;
        }

        private synchronized void messageReceived(long eventGeneration, int payloadBytes) {
            if (eventGeneration != generation) {
                return;
            }
            dailyCounters.resetIfNeeded(LocalDate.now(clock));
            receivedMessages++;
            receivedBytes += Math.max(payloadBytes, 0);
            dailyCounters.messageReceived(payloadBytes);
            lastMessageAt = clock.instant();
        }

        private synchronized void messageSent(long eventGeneration, int payloadBytes) {
            if (eventGeneration != generation) {
                return;
            }
            dailyCounters.resetIfNeeded(LocalDate.now(clock));
            sentMessages++;
            sentBytes += Math.max(payloadBytes, 0);
            dailyCounters.messageSent(payloadBytes);
        }

        private synchronized ConnectionSnapshot snapshot() {
            dailyCounters.resetIfNeeded(LocalDate.now(clock));
            return ConnectionSnapshot.builder()
                    .connectionId(connectionId)
                    .generation(generation)
                    .lastSequence(lastSequence)
                    .state(state)
                    .stateChangedAt(stateChangedAt)
                    .connectedAt(connectedAt)
                    .readyAt(readyAt)
                    .lastMessageAt(lastMessageAt)
                    .lastErrorAt(lastErrorAt)
                    .lastErrorCategory(lastErrorCategory)
                    .lastErrorCode(lastErrorCode)
                    .lastErrorMessage(lastErrorMessage)
                    .reconnectAttempts(reconnectAttempts)
                    .receivedMessages(receivedMessages)
                    .receivedBytes(receivedBytes)
                    .sentMessages(sentMessages)
                    .sentBytes(sentBytes)
                    .today(dailyCounters.snapshot())
                    .build();
        }

        private LocalDate dateOf(Instant instant) {
            return instant.atZone(statisticsZone).toLocalDate();
        }
    }

    private static final class DailyConnectionCounters {
        private LocalDate date;
        private long connectAttempts;
        private long transportConnections;
        private long readyTransitions;
        private long disconnects;
        private long connectionLosses;
        private long retries;
        private long errors;
        private long heartbeatTimeouts;
        private long receivedMessages;
        private long receivedBytes;
        private long sentMessages;
        private long sentBytes;

        private DailyConnectionCounters(LocalDate date) {
            this.date = date;
        }

        private void connectAttempted() { connectAttempts++; }
        private void transportConnected() { transportConnections++; }
        private void becameReady() { readyTransitions++; }
        private void disconnectRequested() { disconnects++; }
        private void connectionLost() { connectionLosses++; }
        private void retryScheduled() { retries++; }
        private void errorOccurred() { errors++; }
        private void heartbeatTimedOut() { heartbeatTimeouts++; }

        private void messageReceived(int payloadBytes) {
            receivedMessages++;
            receivedBytes += Math.max(payloadBytes, 0);
        }

        private void messageSent(int payloadBytes) {
            sentMessages++;
            sentBytes += Math.max(payloadBytes, 0);
        }

        private void resetIfNeeded(LocalDate currentDate) {
            if (currentDate.equals(date)) return;
            date = currentDate;
            connectAttempts = 0;
            transportConnections = 0;
            readyTransitions = 0;
            disconnects = 0;
            connectionLosses = 0;
            retries = 0;
            errors = 0;
            heartbeatTimeouts = 0;
            receivedMessages = 0;
            receivedBytes = 0;
            sentMessages = 0;
            sentBytes = 0;
        }

        private ConnectionDailySnapshot snapshot() {
            return ConnectionDailySnapshot.builder()
                    .date(date)
                    .connectAttempts(connectAttempts)
                    .transportConnections(transportConnections)
                    .readyTransitions(readyTransitions)
                    .disconnects(disconnects)
                    .connectionLosses(connectionLosses)
                    .retries(retries)
                    .errors(errors)
                    .heartbeatTimeouts(heartbeatTimeouts)
                    .receivedMessages(receivedMessages)
                    .receivedBytes(receivedBytes)
                    .sentMessages(sentMessages)
                    .sentBytes(sentBytes)
                    .build();
        }
    }
}

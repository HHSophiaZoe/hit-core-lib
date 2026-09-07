package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.FailureCategory;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.TransportFailureException;
import com.hit.websocket.client.transport.WebSocketFrame;
import com.hit.websocket.client.transport.WebSocketTransport;
import com.hit.websocket.client.transport.WebSocketTransportFactory;
import com.hit.websocket.client.transport.WebSocketTransportListener;
import com.hit.websocket.client.observability.model.ConnectionEvent;
import com.hit.websocket.client.observability.model.ConnectionEventDetails;
import com.hit.websocket.client.observability.model.ConnectionEventType;
import com.hit.websocket.client.observability.ConnectionObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Transport-neutral connection lifecycle, reconnect and heartbeat supervisor. */
final class WebSocketClientManager implements ManagedWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientManager.class);

    private final Object monitor = new Object();
    private final WebSocketClientOptions options;
    private final WebSocketClientListener listener;
    private final WebSocketTransportFactory transportFactory;
    private final ScheduledExecutorService scheduler;
    private final List<ConnectionObserver> observers;
    private final Clock clock;

    private volatile ConnectionState state = ConnectionState.STOPPED;
    private volatile WebSocketTransport transport;
    private volatile boolean desiredRunning;
    private volatile Supplier<WebSocketFrame> heartbeatFrameSupplier;
    private final AtomicLong generation = new AtomicLong();
    private long sequence;
    private int reconnectAttempts;
    private volatile boolean awaitingPong;
    private volatile long lastPingNanos;
    private ScheduledFuture<?> retryFuture;
    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> pongTimeoutFuture;
    private ScheduledFuture<?> lifecycleStageTimeoutFuture;

    WebSocketClientManager(
            WebSocketClientOptions options,
            WebSocketClientListener listener,
            WebSocketTransportFactory transportFactory,
            ScheduledExecutorService scheduler,
            List<ConnectionObserver> observers,
            Clock clock
    ) {
        this.options = Objects.requireNonNull(options);
        this.listener = Objects.requireNonNull(listener);
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.observers = List.copyOf(observers);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void connect() {
        Attempt attempt;
        synchronized (monitor) {
            desiredRunning = true;
            if (isConnectionInProgress(state)) {
                return;
            }
            cancelRetryLocked();
            attempt = createAttemptLocked();
        }
        publish(attempt.event());
        open(attempt);
    }

    @Override
    public void disconnect() {
        WebSocketTransport current;
        ConnectionEvent disconnecting;
        long stopGeneration;
        synchronized (monitor) {
            if (!desiredRunning
                    && (state == ConnectionState.STOPPED || state == ConnectionState.DISCONNECTING)) {
                return;
            }
            desiredRunning = false;
            cancelRetryLocked();
            cancelHeartbeatLocked();
            cancelLifecycleStageTimeoutLocked();
            awaitingPong = false;
            current = transport;
            transport = null;
            stopGeneration = generation.incrementAndGet();
            sequence = 0;
            state = ConnectionState.DISCONNECTING;
            disconnecting = eventLocked(ConnectionEventType.DISCONNECT_REQUESTED);
        }
        publish(disconnecting);

        CompletionStage<Void> closeStage = safeDisconnect(current, CloseReason.normal());
        closeStage.whenComplete((ignored, error) -> finishStopped(stopGeneration, error));
    }

    @Override
    public CompletionStage<Void> send(WebSocketFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        WebSocketTransport current;
        long currentGeneration;
        synchronized (monitor) {
            current = transport;
            currentGeneration = generation.get();
        }
        if (current == null || !current.isOpen()) {
            return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not connected"));
        }
        CompletionStage<Void> result;
        try {
            result = current.send(frame);
        } catch (RuntimeException error) {
            result = CompletableFuture.failedFuture(error);
        }
        result.whenComplete((ignored, error) -> {
            if (error == null) {
                if (!observers.isEmpty()) notifyMessageSent(currentGeneration, frame.payloadSize());
            } else {
                terminate(currentGeneration, transportFailure(error), null);
            }
        });
        return result;
    }

    @Override
    public boolean isConnected() {
        WebSocketTransport current = transport;
        return state == ConnectionState.READY && current != null && current.isOpen();
    }

    @Override
    public boolean isHealthy() {
        WebSocketTransport current = transport;
        if (current == null || !current.isOpen() || state != ConnectionState.READY) {
            return false;
        }
        Duration pongTimeout = options.pongTimeout();
        return !awaitingPong || pongTimeout == null
                || System.nanoTime() - lastPingNanos < pongTimeout.toNanos();
    }

    @Override
    public ConnectionState state() {
        return state;
    }

    @Override
    public void setHeartbeatFrameSupplier(Supplier<WebSocketFrame> supplier) {
        heartbeatFrameSupplier = supplier;
    }

    @Override
    public void markPongReceived() {
        synchronized (monitor) {
            awaitingPong = false;
            if (pongTimeoutFuture != null) {
                pongTimeoutFuture.cancel(false);
                pongTimeoutFuture = null;
            }
        }
    }

    @Override
    public void markAuthenticating() {
        transition(ConnectionState.AUTHENTICATING, ConnectionEventType.AUTHENTICATION_STARTED);
    }

    @Override
    public void markResubscribing() {
        transition(ConnectionState.RESUBSCRIBING, ConnectionEventType.RESUBSCRIBE_STARTED);
    }

    @Override
    public void markReady() {
        synchronized (monitor) {
            reconnectAttempts = 0;
        }
        transition(ConnectionState.READY, ConnectionEventType.READY);
    }

    @Override
    public void reportError(FailureCategory category, String code, String message) {
        ConnectionEvent event;
        synchronized (monitor) {
            event = eventLocked(ConnectionEventType.ERROR, failureDetails(
                    new TransportFailure(category, code, message, false), false));
        }
        publish(event);
        safelyNotifyError(new TransportFailure(category, code, message, false));
    }

    @Override
    public void fail(TransportFailure failure) {
        Objects.requireNonNull(failure, "failure cannot be null");
        terminate(generation.get(), failure, null);
    }

    private Attempt createAttemptLocked() {
        WebSocketTransport created = transportFactory.create(options.connectionId());
        transport = created;
        long attemptGeneration = generation.incrementAndGet();
        sequence = 0;
        state = ConnectionState.CONNECTING;
        ConnectionEvent event = eventLocked(ConnectionEventType.CONNECT_REQUESTED,
                new ConnectionEventDetails.ConnectAttempt(reconnectAttempts));
        return new Attempt(attemptGeneration, created, event);
    }

    private void open(Attempt attempt) {
        synchronized (monitor) {
            if (!desiredRunning || !isCurrentLocked(attempt.generation(), attempt.transport())) {
                safeDisconnect(attempt.transport(), CloseReason.normal());
                return;
            }
        }
        WebSocketTransportListener transportListener = new WebSocketTransportListener() {
            @Override
            public void onFrame(WebSocketFrame frame) {
                handleFrame(attempt.generation(), frame);
            }

            @Override
            public void onClosed(CloseReason reason) {
                terminate(attempt.generation(), null, reason);
            }

            @Override
            public void onFailure(TransportFailure failure) {
                terminate(attempt.generation(), failure, null);
            }
        };

        try {
            attempt.transport().connect(options.connectRequest(), transportListener)
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            terminate(attempt.generation(), transportFailure(error), null);
                        } else {
                            connected(attempt);
                        }
                    });
        } catch (RuntimeException error) {
            terminate(attempt.generation(), TransportFailure.unknown(error), null);
        }
    }

    private void connected(Attempt attempt) {
        ConnectionEvent event;
        synchronized (monitor) {
            if (!isCurrentLocked(attempt.generation(), attempt.transport()) || !desiredRunning) {
                safeDisconnect(attempt.transport(), CloseReason.normal());
                return;
            }
            // An eager transport may deliver welcome/auth before connect() returns its stage.
            // Do not move an already advanced protocol back to TRANSPORT_CONNECTED.
            if (state != ConnectionState.CONNECTING) return;
            state = ConnectionState.TRANSPORT_CONNECTED;
            event = eventLocked(ConnectionEventType.TRANSPORT_CONNECTED);
            startLifecycleStageTimeoutLocked(attempt.generation(), ConnectionState.TRANSPORT_CONNECTED);
        }
        publish(event);
        try {
            listener.onTransportConnected();
        } catch (RuntimeException error) {
            terminate(attempt.generation(), new TransportFailure(
                    FailureCategory.PROTOCOL, "CONNECTED_CALLBACK_FAILED", error.getMessage(), true), null);
        }
    }

    private void handleFrame(long eventGeneration, WebSocketFrame frame) {
        synchronized (monitor) {
            if (eventGeneration != generation.get() || transport == null) {
                return;
            }
        }
        if (!observers.isEmpty()) notifyMessageReceived(eventGeneration, frame.payloadSize());
        if (frame instanceof WebSocketFrame.Pong) {
            markPongReceived();
            return;
        }
        if (frame instanceof WebSocketFrame.Ping ping) {
            send(new WebSocketFrame.Pong(ping.payload()));
            return;
        }
        try {
            listener.onMessage(frame);
        } catch (RuntimeException error) {
            reportError(FailureCategory.PROTOCOL, "MESSAGE_CALLBACK_FAILED", error.getMessage());
        }
    }

    private void terminate(long eventGeneration, TransportFailure failure, CloseReason closeReason) {
        WebSocketTransport terminatedTransport;
        ConnectionEvent terminalEvent;
        synchronized (monitor) {
            if (eventGeneration != generation.get() || transport == null
                    || state == ConnectionState.RETRY_WAIT || state == ConnectionState.FAILED) {
                return;
            }
            terminatedTransport = transport;
            transport = null;
            if (failure == null && isNormalServerClose(closeReason)) desiredRunning = false;
            cancelHeartbeatLocked();
            cancelLifecycleStageTimeoutLocked();
            awaitingPong = false;
            if (failure != null) {
                terminalEvent = eventLocked(ConnectionEventType.ERROR, failureDetails(failure, true));
            } else {
                CloseReason reason = closeReason == null
                        ? CloseReason.unavailable("Connection closed") : closeReason;
                terminalEvent = eventLocked(ConnectionEventType.TRANSPORT_CLOSED,
                        new ConnectionEventDetails.TransportClosed(reason));
            }
        }
        publish(terminalEvent);
        if (failure != null) {
            safelyNotifyError(failure);
        } else {
            safelyNotifyDisconnected(closeReason == null
                    ? CloseReason.unavailable("Connection closed") : closeReason);
        }
        safeDisconnect(terminatedTransport, CloseReason.normal());
        scheduleRetry(eventGeneration, shouldRetry(failure, closeReason));
    }

    private void scheduleRetry(long terminatedGeneration, boolean retryable) {
        ConnectionEvent event;
        int attempt = 0;
        Duration delay = Duration.ZERO;
        boolean scheduled = false;
        synchronized (monitor) {
            // A listener may already have disconnected and started another generation.
            if (terminatedGeneration != generation.get()) return;
            if (!desiredRunning) {
                state = ConnectionState.STOPPED;
                event = eventLocked(ConnectionEventType.STOPPED);
            } else if (!retryable) {
                state = ConnectionState.FAILED;
                event = eventLocked(ConnectionEventType.FAILED,
                        new ConnectionEventDetails.LifecycleFailed(
                                ConnectionEventDetails.LifecycleFailed.Reason.NON_RETRYABLE_FAILURE, null));
            } else {
                attempt = reconnectAttempts + 1;
                if (!options.retryPolicy().allows(attempt)) {
                    state = ConnectionState.FAILED;
                    event = eventLocked(ConnectionEventType.FAILED,
                            new ConnectionEventDetails.LifecycleFailed(
                                    ConnectionEventDetails.LifecycleFailed.Reason.RETRY_EXHAUSTED, attempt));
                } else {
                    reconnectAttempts = attempt;
                    delay = options.retryPolicy().delayFor(attempt);
                    long delayMillis = delay.toMillis();
                    state = ConnectionState.RETRY_WAIT;
                    event = eventLocked(ConnectionEventType.RETRY_SCHEDULED,
                            new ConnectionEventDetails.RetryScheduled(
                                    attempt, options.retryPolicy().maxAttempts(), delay));
                    retryFuture = scheduler.schedule(
                            () -> retry(terminatedGeneration), delayMillis, TimeUnit.MILLISECONDS);
                    scheduled = true;
                }
            }
        }
        publish(event);
        if (scheduled) {
            safelyNotifyReconnecting(attempt, delay);
        }
    }

    private void retry(long expectedGeneration) {
        Attempt attempt;
        synchronized (monitor) {
            if (!desiredRunning || expectedGeneration != generation.get()
                    || state != ConnectionState.RETRY_WAIT) {
                return;
            }
            retryFuture = null;
            attempt = createAttemptLocked();
        }
        publish(attempt.event());
        open(attempt);
    }

    private void startHeartbeatLocked(long heartbeatGeneration) {
        cancelHeartbeatLocked();
        if (options.heartbeatInterval() == null || heartbeatFrameSupplier == null) {
            return;
        }
        long intervalMillis = options.heartbeatInterval().toMillis();
        heartbeatFuture = scheduler.scheduleWithFixedDelay(
                () -> heartbeatTick(heartbeatGeneration),
                intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void heartbeatTick(long heartbeatGeneration) {
        Supplier<WebSocketFrame> supplier;
        synchronized (monitor) {
            if (heartbeatGeneration != generation.get() || transport == null || !desiredRunning) {
                return;
            }
            if (awaitingPong) {
                return;
            } else {
                supplier = heartbeatFrameSupplier;
                if (supplier == null) return;
                awaitingPong = true;
                lastPingNanos = System.nanoTime();
                pongTimeoutFuture = scheduler.schedule(
                        () -> heartbeatTimedOut(heartbeatGeneration),
                        options.pongTimeout().toMillis(), TimeUnit.MILLISECONDS);
            }
        }
        try {
            send(supplier.get());
        } catch (RuntimeException error) {
            terminate(heartbeatGeneration, TransportFailure.unknown(error), null);
        }
    }

    private void heartbeatTimedOut(long heartbeatGeneration) {
        ConnectionEvent timeoutEvent;
        synchronized (monitor) {
            if (heartbeatGeneration != generation.get() || transport == null || !desiredRunning || !awaitingPong) {
                return;
            }
            pongTimeoutFuture = null;
            timeoutEvent = eventLocked(ConnectionEventType.HEARTBEAT_TIMEOUT,
                    new ConnectionEventDetails.Failure(
                            FailureCategory.NETWORK, "PONG_TIMEOUT",
                            "Pong was not received before the configured timeout", true, false));
        }
        publish(timeoutEvent);
        terminate(heartbeatGeneration, new TransportFailure(
                FailureCategory.NETWORK, "PONG_TIMEOUT", "Heartbeat pong timeout", true), null);
    }

    private void transition(ConnectionState target, ConnectionEventType type) {
        ConnectionEvent event;
        synchronized (monitor) {
            if (transport == null || !desiredRunning) {
                return;
            }
            state = target;
            if (target == ConnectionState.READY) {
                cancelLifecycleStageTimeoutLocked();
                startHeartbeatLocked(generation.get());
            } else {
                startLifecycleStageTimeoutLocked(generation.get(), target);
            }
            event = eventLocked(type);
        }
        publish(event);
    }

    private void finishStopped(long stopGeneration, Throwable error) {
        ConnectionEvent event;
        synchronized (monitor) {
            if (stopGeneration != generation.get() || desiredRunning) {
                return;
            }
            state = ConnectionState.STOPPED;
            event = error == null
                    ? eventLocked(ConnectionEventType.STOPPED)
                    : eventLocked(ConnectionEventType.STOPPED,
                            new ConnectionEventDetails.CloseFailed(safeMessage(error)));
        }
        publish(event);
    }

    private boolean isCurrentLocked(long expectedGeneration, WebSocketTransport expectedTransport) {
        return generation.get() == expectedGeneration && transport == expectedTransport;
    }

    private static boolean isConnectionInProgress(ConnectionState state) {
        return state == ConnectionState.CONNECTING
                || state == ConnectionState.TRANSPORT_CONNECTED
                || state == ConnectionState.AUTHENTICATING
                || state == ConnectionState.RESUBSCRIBING
                || state == ConnectionState.READY
                || state == ConnectionState.RETRY_WAIT;
    }

    private ConnectionEvent eventLocked(ConnectionEventType type) {
        return eventLocked(type, ConnectionEventDetails.None.INSTANCE);
    }

    private ConnectionEvent eventLocked(ConnectionEventType type, ConnectionEventDetails details) {
        return new ConnectionEvent(options.connectionId(), generation.get(), ++sequence,
                type, state, clock.instant(), details);
    }

    private void publish(ConnectionEvent event) {
        for (ConnectionObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException error) {
                log.warn("WebSocket connection observer failed for {}", options.connectionId().value(), error);
            }
        }
    }

    private void notifyMessageReceived(long eventGeneration, int payloadBytes) {
        for (ConnectionObserver observer : observers) {
            try {
                observer.onMessageReceived(options.connectionId(), eventGeneration, payloadBytes);
            } catch (RuntimeException error) {
                log.warn("WebSocket receive observer failed for {}", options.connectionId().value(), error);
            }
        }
    }

    private void notifyMessageSent(long eventGeneration, int payloadBytes) {
        for (ConnectionObserver observer : observers) {
            try {
                observer.onMessageSent(options.connectionId(), eventGeneration, payloadBytes);
            } catch (RuntimeException error) {
                log.warn("WebSocket send observer failed for {}", options.connectionId().value(), error);
            }
        }
    }

    private void safelyNotifyError(TransportFailure failure) {
        try {
            listener.onError(failure);
        } catch (RuntimeException error) {
            log.warn("WebSocket error listener failed for {}", options.connectionId().value(), error);
        }
    }

    private void safelyNotifyDisconnected(CloseReason reason) {
        try {
            listener.onTransportDisconnected(reason);
        } catch (RuntimeException error) {
            log.warn("WebSocket disconnect listener failed for {}", options.connectionId().value(), error);
        }
    }

    private void safelyNotifyReconnecting(int attempt, Duration delay) {
        try {
            listener.onReconnecting(attempt, options.retryPolicy().maxAttempts(), delay);
        } catch (RuntimeException error) {
            log.warn("WebSocket reconnect listener failed for {}", options.connectionId().value(), error);
        }
    }

    private void cancelRetryLocked() {
        if (retryFuture != null) {
            retryFuture.cancel(false);
            retryFuture = null;
        }
    }

    private void cancelHeartbeatLocked() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
        if (pongTimeoutFuture != null) {
            pongTimeoutFuture.cancel(false);
            pongTimeoutFuture = null;
        }
    }

    private void startLifecycleStageTimeoutLocked(long timeoutGeneration, ConnectionState expectedState) {
        cancelLifecycleStageTimeoutLocked();
        Duration timeout = options.lifecycleStageTimeout();
        if (timeout == null || expectedState == ConnectionState.READY) return;
        lifecycleStageTimeoutFuture = scheduler.schedule(
                () -> lifecycleStageTimedOut(timeoutGeneration, expectedState), timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void lifecycleStageTimedOut(long timeoutGeneration, ConnectionState expectedState) {
        synchronized (monitor) {
            if (timeoutGeneration != generation.get() || state != expectedState || transport == null || !desiredRunning) return;
            lifecycleStageTimeoutFuture = null;
        }
        String code = expectedState.name() + "_TIMEOUT";
        terminate(timeoutGeneration, new TransportFailure(
                FailureCategory.PROTOCOL, code, "WebSocket lifecycle stage timed out: " + expectedState, true), null);
    }

    private void cancelLifecycleStageTimeoutLocked() {
        if (lifecycleStageTimeoutFuture == null) return;
        lifecycleStageTimeoutFuture.cancel(false);
        lifecycleStageTimeoutFuture = null;
    }

    private static boolean shouldRetry(TransportFailure failure, CloseReason closeReason) {
        if (failure != null) return failure.retryable();
        if (closeReason == null) return true;
        if (closeReason.initiatedByClient()) return false;
        return closeReason.code() != 1000 && closeReason.code() != 1001;
    }

    private static boolean isNormalServerClose(CloseReason closeReason) {
        return closeReason != null && !closeReason.initiatedByClient()
                && (closeReason.code() == 1000 || closeReason.code() == 1001);
    }

    private static ConnectionEventDetails.Failure failureDetails(
            TransportFailure failure, boolean terminal
    ) {
        return new ConnectionEventDetails.Failure(
                failure.category(), failure.code(), failure.message(), failure.retryable(), terminal);
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static TransportFailure transportFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TransportFailureException failureException) return failureException.failure();
            current = current.getCause();
        }
        return TransportFailure.unknown(error);
    }

    private static CompletionStage<Void> safeDisconnect(WebSocketTransport current, CloseReason reason) {
        if (current == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return current.disconnect(reason);
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private record Attempt(long generation, WebSocketTransport transport, ConnectionEvent event) {
    }
}

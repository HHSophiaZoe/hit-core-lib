package com.hit.websocket.client.connection;

import com.hit.websocket.client.observability.WebSocketObserver;
import com.hit.websocket.client.observability.model.ConnectionEvent;
import com.hit.websocket.client.observability.model.ConnectionEventDetails;
import com.hit.websocket.client.observability.model.ConnectionEventType;
import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.FailureCategory;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.TransportFailureException;
import com.hit.websocket.client.transport.WebSocketFrame;
import com.hit.websocket.client.transport.WebSocketTransport;
import com.hit.websocket.client.transport.WebSocketTransportListener;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One control thread owns lifecycle state, timers and callbacks. Commands are always
 * queued, including commands issued by callbacks, so transitions cannot be reentrant.
 * Application work belongs in MessageDispatcher.
 */
@Slf4j
final class DefaultWebSocketClient implements WebSocketClient {
    private final WebSocketClientOptions options;
    private final WebSocketClientListener listener;
    private final WebSocketClientFactory factory;
    private final ScheduledThreadPoolExecutor control;
    private final Semaphore inboundCapacity;
    private volatile ConnectionState state = ConnectionState.STOPPED;
    private volatile Attempt active;
    private volatile boolean closed;
    private volatile long pongDeadline;
    private long generation;
    private long sequence;
    private int retries;
    private ScheduledFuture<?> retryTask;
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> deadlineTask;
    private Supplier<WebSocketFrame> heartbeat = () -> new WebSocketFrame.Ping(null);

    DefaultWebSocketClient(WebSocketClientOptions options, WebSocketClientListener listener,
                           WebSocketClientFactory factory) {
        this.options = Objects.requireNonNull(options);
        this.listener = Objects.requireNonNull(listener);
        this.factory = factory;
        inboundCapacity = new Semaphore(options.inboundCapacity());
        control = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "websocket-" + options.connectionId().value());
            thread.setDaemon(true);
            return thread;
        });
        control.setRemoveOnCancelPolicy(true);
        control.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    @Override
    public void connect() {
        if (closed) throw new IllegalStateException("WebSocket client is closed");
        execute(() -> {
            if (state != ConnectionState.STOPPED && state != ConnectionState.FAILED) return;
            retries = 0;
            open();
        });
    }

    @Override
    public void disconnect() {
        execute(this::stop);
    }

    @Override
    public void close() {
        execute(() -> {
            if (closed) return;
            closed = true;
            stop();
            control.shutdown();
            factory.release(options.connectionId(), this);
        });
    }

    @Override
    public CompletionStage<Void> send(WebSocketFrame frame) {
        Objects.requireNonNull(frame, "frame");
        Attempt attempt = active;
        if (attempt == null || !attempt.transport.isOpen()) {
            return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not connected"));
        }
        if (!attempt.pendingSends.tryAcquire()) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("WebSocket pending send limit reached"));
        }
        try {
            CompletionStage<Void> sent = attempt.transport.send(frame);
            sent.whenComplete((ignored, error) -> execute(() -> {
                try {
                    if (active != attempt) return;
                    if (error != null) terminate(attempt, failure(error), null);
                    else observe(observer -> observer.onMessageSent(options.connectionId(), attempt.generation, frame.payloadSize()));
                } finally {
                    attempt.pendingSends.release();
                }
            }));
            return sent;
        } catch (RuntimeException error) {
            attempt.pendingSends.release();
            execute(() -> terminate(attempt, failure(error), null));
            return CompletableFuture.failedFuture(error);
        }
    }

    @Override
    public boolean isConnected() {
        Attempt attempt = active;
        return state == ConnectionState.CONNECTED && attempt != null && attempt.transport.isOpen();
    }

    @Override
    public boolean isHealthy() {
        long deadline = pongDeadline;
        return isConnected() && (deadline == 0 || System.nanoTime() - deadline < 0);
    }

    @Override
    public ConnectionState state() {
        return state;
    }

    @Override
    public void markPongReceived() {
        Attempt attempt = active;
        execute(() -> pong(attempt));
    }

    private void pong(Attempt attempt) {
        if (active != attempt || pongDeadline == 0) return;
        cancel(deadlineTask);
        pongDeadline = 0;
        scheduleHeartbeat(attempt);
    }

    @Override
    public void setHeartbeatFrameSupplier(Supplier<WebSocketFrame> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        execute(() -> heartbeat = supplier);
    }

    @Override
    public void reportError(FailureCategory category, String code, String message) {
        Attempt attempt = active;
        execute(() -> {
            if (attempt == null || active != attempt) return;
            TransportFailure error = new TransportFailure(category, code, message, false);
            emit(ConnectionEventType.ERROR, details(error, false));
            notifyListener(() -> listener.onError(error));
        });
    }

    @Override
    public void fail(TransportFailure failure) {
        Objects.requireNonNull(failure, "failure");
        Attempt attempt = active;
        execute(() -> terminate(attempt, failure, null));
    }

    private void open() {
        if (closed) return;
        generation = factory.nextGeneration();
        sequence = 0;
        state = ConnectionState.CONNECTING;
        emit(ConnectionEventType.CONNECT_REQUESTED, new ConnectionEventDetails.ConnectAttempt(retries));
        try {
            Attempt attempt = new Attempt(generation, factory.transportFactory().create(options.connectionId()), options.inboundCapacity());
            active = attempt;
            deadlineTask = later(options.connectRequest().connectTimeout(),
                    () -> terminate(attempt, timeout("CONNECT_TIMEOUT"), null));
            attempt.transport.connect(options.connectRequest(), new WebSocketTransportListener() {
                @Override
                public void onFrame(WebSocketFrame frame) {
                    receive(attempt, frame);
                }

                @Override
                public void onClosed(CloseReason reason) {
                    execute(() -> terminate(attempt, null, reason));
                }

                @Override
                public void onFailure(TransportFailure failure) {
                    execute(() -> terminate(attempt, failure, null));
                }
            }).whenComplete((ignored, error) -> execute(() -> {
                if (error == null) connected(attempt);
                else terminate(attempt, failure(error), null);
            }));
        } catch (RuntimeException error) {
            if (active != null) terminate(active, failure(error), null);
            else {
                emit(ConnectionEventType.ERROR, details(failure(error), true));
                scheduleRetry(true);
                notifyListener(() -> listener.onError(failure(error)));
            }
        }
    }

    private void connected(Attempt attempt) {
        if (active != attempt || state != ConnectionState.CONNECTING) return;
        cancel(deadlineTask);
        attempt.connectedAtNanos = System.nanoTime();
        state = ConnectionState.CONNECTED;
        emit(ConnectionEventType.TRANSPORT_CONNECTED);
        scheduleHeartbeat(attempt);
        try {
            listener.onTransportConnected();
        } catch (RuntimeException error) {
            terminate(attempt, failure(error), null);
        }
    }

    private void receive(Attempt attempt, WebSocketFrame frame) {
        if (active != attempt || attempt.overflow.get()) return;
        if (!inboundCapacity.tryAcquire()) {
            if (attempt.overflow.compareAndSet(false, true)) {
                execute(() -> terminate(attempt, new TransportFailure(FailureCategory.PROTOCOL,
                        "INBOUND_OVERFLOW", "Use MessageDispatcher for application work", true), null));
            }
            return;
        }
        execute(() -> {
            try {
                if (active != attempt) return;
                connected(attempt);
                if (active != attempt) return;
                observe(observer -> observer.onMessageReceived(options.connectionId(), attempt.generation, frame.payloadSize()));
                if (frame instanceof WebSocketFrame.Pong) pong(attempt);
                else if (frame instanceof WebSocketFrame.Ping ping) send(new WebSocketFrame.Pong(ping.payload()));
                else listener.onMessage(frame);
            } catch (RuntimeException error) {
                terminate(attempt, failure(error), null);
            } finally {
                inboundCapacity.release();
            }
        });
    }

    private void scheduleHeartbeat(Attempt attempt) {
        if (options.heartbeat() == null || active != attempt) return;
        cancel(heartbeatTask);
        heartbeatTask = later(options.heartbeat().interval(), () -> {
            if (active != attempt || state != ConnectionState.CONNECTED) return;
            try {
                WebSocketFrame frame = heartbeat.get();
                if (frame == null) {
                    scheduleHeartbeat(attempt);
                    return;
                }
                pongDeadline = System.nanoTime() + options.heartbeat().pongTimeout().toNanos();
                deadlineTask = later(options.heartbeat().pongTimeout(), () -> {
                    emit(ConnectionEventType.HEARTBEAT_TIMEOUT);
                    terminate(attempt, timeout("PONG_TIMEOUT"), null);
                });
                send(frame);
            } catch (RuntimeException error) {
                terminate(attempt, failure(error), null);
            }
        });
    }

    private void terminate(Attempt attempt, TransportFailure error, CloseReason reason) {
        if (attempt == null || active != attempt) return;
        active = null;
        cancelTimers();
        // A stable connection resets the backoff. Rapid connect/drop loops retain their retry budget.
        if (attempt.connectedAtNanos != 0 && System.nanoTime() - attempt.connectedAtNanos >= TimeUnit.SECONDS.toNanos(30)) retries = 0;
        if (error != null) emit(ConnectionEventType.ERROR, details(error, true));
        else emit(ConnectionEventType.TRANSPORT_CLOSED, new ConnectionEventDetails.TransportClosed(reason));
        closeTransport(attempt);
        scheduleRetry(error != null ? error.retryable() : !reason.initiatedByClient());
        if (error != null) notifyListener(() -> listener.onError(error));
        CloseReason disconnected = reason == null ? CloseReason.unavailable("Transport failed") : reason;
        notifyListener(() -> listener.onTransportDisconnected(disconnected));
    }

    private void scheduleRetry(boolean retryable) {
        int nextAttempt = retries + 1;
        if (!retryable || !options.reconnectPolicy().allows(nextAttempt)) {
            state = ConnectionState.FAILED;
            emit(ConnectionEventType.FAILED, new ConnectionEventDetails.LifecycleFailed(
                    retryable ? ConnectionEventDetails.LifecycleFailed.Reason.RETRY_EXHAUSTED
                            : ConnectionEventDetails.LifecycleFailed.Reason.NON_RETRYABLE_FAILURE, nextAttempt));
            return;
        }
        retries = nextAttempt;
        Duration delay = options.reconnectPolicy().delayFor(retries);
        state = ConnectionState.RETRY_WAIT;
        emit(ConnectionEventType.RETRY_SCHEDULED,
                new ConnectionEventDetails.RetryScheduled(retries, options.reconnectPolicy().maxAttempts(), delay));
        retryTask = later(delay, this::open);
        notifyListener(() -> listener.onReconnecting(retries, options.reconnectPolicy().maxAttempts(), delay));
    }

    private void stop() {
        cancelTimers();
        Attempt attempt = active;
        active = null;
        if (state == ConnectionState.STOPPED) return;
        state = ConnectionState.DISCONNECTING;
        emit(ConnectionEventType.DISCONNECT_REQUESTED);
        closeTransport(attempt);
        state = ConnectionState.STOPPED;
        emit(ConnectionEventType.STOPPED);
        if (attempt != null) notifyListener(() -> listener.onTransportDisconnected(CloseReason.normal()));
    }

    private void closeTransport(Attempt attempt) {
        if (attempt == null) return;
        try {
            attempt.transport.disconnect(CloseReason.normal()).whenComplete((ignored, error) -> {
                if (error != null) log.warn("WebSocket close failed for {}", options.connectionId().value(), error);
            });
        } catch (RuntimeException error) {
            log.warn("WebSocket close failed for {}", options.connectionId().value(), error);
        }
    }

    private void cancelTimers() {
        cancel(retryTask);
        cancel(heartbeatTask);
        cancel(deadlineTask);
        pongDeadline = 0;
    }

    private static void cancel(ScheduledFuture<?> task) {
        if (task != null) task.cancel(false);
    }

    private ScheduledFuture<?> later(Duration delay, Runnable task) {
        return control.schedule(task, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void execute(Runnable task) {
        try {
            control.execute(task);
        } catch (RejectedExecutionException error) {
            if (!closed) throw error;
        }
    }

    private void emit(ConnectionEventType type) {
        emit(type, ConnectionEventDetails.None.INSTANCE);
    }

    private void emit(ConnectionEventType type, ConnectionEventDetails details) {
        ConnectionEvent event = ConnectionEvent.builder()
                .connectionId(options.connectionId())
                .generation(generation)
                .sequence(++sequence)
                .type(type)
                .state(state)
                .occurredAt(factory.clock().instant())
                .details(details)
                .build();
        observe(observer -> observer.onEvent(event));
    }

    private void observe(Consumer<WebSocketObserver> notification) {
        for (WebSocketObserver observer : factory.observers()) {
            try {
                notification.accept(observer);
            } catch (RuntimeException error) {
                log.warn("WebSocket observer failed for {}", options.connectionId().value(), error);
            }
        }
    }

    private void notifyListener(Runnable notification) {
        try {
            notification.run();
        } catch (RuntimeException error) {
            log.warn("WebSocket listener failed for {}", options.connectionId().value(), error);
        }
    }

    private static TransportFailure timeout(String code) {
        FailureCategory category = code.equals("CONNECT_TIMEOUT") ? FailureCategory.CONNECT_TIMEOUT : FailureCategory.NETWORK;
        return new TransportFailure(category, code, "WebSocket timed out: " + code, true);
    }

    private static TransportFailure failure(Throwable error) {
        Throwable cause = error;
        while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) cause = cause.getCause();
        return cause instanceof TransportFailureException transportError ? transportError.failure() : TransportFailure.unknown(cause);
    }

    private static ConnectionEventDetails.Failure details(TransportFailure error, boolean terminal) {
        return ConnectionEventDetails.Failure.builder()
                .category(error.category())
                .code(error.code())
                .message(error.message())
                .retryable(error.retryable())
                .terminal(terminal)
                .build();
    }

    private static final class Attempt {
        private final long generation;
        private final WebSocketTransport transport;
        private final Semaphore pendingSends;
        private long connectedAtNanos;
        private final AtomicBoolean overflow = new AtomicBoolean();

        private Attempt(long generation, WebSocketTransport transport, int capacity) {
            this.generation = generation;
            this.transport = Objects.requireNonNull(transport);
            this.pendingSends = new Semaphore(capacity);
        }
    }
}

package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Một kết nối logic dùng lâu dài cho provider giới hạn thời gian sống của kết nối (ví dụ Binance ngắt sau 24h).
 * Định kỳ mở session mới, chờ adapter báo sẵn sàng (markReady sau khi đăng ký xong) rồi mới cho session cũ nghỉ,
 * nên không có khoảng trống dữ liệu. Trong lúc hai session cùng nhận, message trùng nội dung chỉ được giao một lần.
 *
 * <p>Mỗi session là một WebSocketClient bình thường nên vẫn tự kết nối lại khi rớt mạng; khi đó listener nhận
 * onDisconnected/onConnected để bù dữ liệu. Mọi trạng thái và callback chạy trên một thread control, giống
 * DefaultWebSocketClient, nên không cần khóa; listener chỉ nên chuyển việc sang MessageDispatcher.
 */
@Slf4j
public final class RotatingWebSocketClient implements AutoCloseable {

    private final WebSocketClientOptions options;
    private final RotationOptions rotation;
    private final RotatingWebSocketListener listener;
    private final WebSocketClientFactory factory;
    private final ScheduledThreadPoolExecutor control;
    private final RecentMessages recentMessages;

    // Chỉ truy cập trên thread control.
    private Session active;     // Session đang dùng.
    private Session standby;    // Session mới, chờ sẵn sàng để thay active.
    private Session retiring;   // Session cũ, còn nhận nốt message trong drainDelay.
    private boolean connected;  // active đã sẵn sàng và chưa bị ngắt.
    private boolean closed;
    private Long dedupeUntil;   // nanoTime; vẫn lọc trùng một lúc sau khi session cũ đóng.
    private char nextSuffix = 'a';
    private ScheduledFuture<?> rotationTask;

    // Bản đọc cho thread khác.
    private volatile List<RotatingSession> openSessions = List.of();
    private volatile boolean connectedView;

    RotatingWebSocketClient(WebSocketClientOptions options, RotationOptions rotation,
                            RotatingWebSocketListener listener, WebSocketClientFactory factory) {
        this.options = Objects.requireNonNull(options);
        this.rotation = Objects.requireNonNull(rotation);
        this.listener = Objects.requireNonNull(listener);
        this.factory = factory;
        this.recentMessages = new RecentMessages(rotation.dedupeWindow().toNanos());
        control = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "websocket-rotation-" + options.connectionId().value());
            thread.setDaemon(true);
            return thread;
        });
        control.setRemoveOnCancelPolicy(true);
        control.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public void connect() {
        execute(() -> {
            if (!closed && active == null) {
                active = open();
                updateViews();
            }
        });
    }

    @Override
    public void close() {
        execute(() -> {
            if (closed) return;
            closed = true;
            cancel(rotationTask);
            Stream.of(active, standby, retiring).filter(Objects::nonNull).forEach(session -> session.client.close());
            active = standby = retiring = null;
            connected = false;
            updateViews();
            control.shutdown();
            factory.release(this);
        });
    }

    public boolean isConnected() {
        return connectedView;
    }

    /** Session đang dùng và session mới (khi đang xoay); gửi thay đổi đăng ký tới tất cả để hai session không lệch nhau. */
    public List<RotatingSession> sessions() {
        return openSessions;
    }

    private Session open() {
        ConnectionId base = options.connectionId();
        ConnectionId id = new ConnectionId(base.provider(), base.name() + "#" + nextSuffix);
        nextSuffix = nextSuffix == 'a' ? 'b' : 'a';
        Session session = new Session();
        session.client = factory.create(new WebSocketClientOptions(id, options.connectRequest(), options.reconnectPolicy(),
                options.heartbeat(), options.inboundCapacity()), session);
        session.client.connect();
        return session;
    }

    private void opened(Session session) {
        if (closed || !isLive(session)) return;
        session.ready = false;
        listener.onSessionOpened(session);
    }

    private void ready(Session session) {
        if (closed || session.ready || (session != active && session != standby)) return;
        session.ready = true;
        if (session == standby) {
            promoteStandby();
        } else if (!connected) {
            markConnected();
        }
    }

    private void lost(Session session) {
        if (closed) return;
        session.ready = false;
        if (session == standby) {
            log.warn("New session of {} closed before it was ready", options.connectionId().value());
            abandonStandby();
        } else if (session == active && connected) {
            connected = false;
            updateViews();
            listener.onDisconnected();
        }
    }

    private void message(Session session, WebSocketFrame frame) {
        if (closed || !isLive(session)) return;
        if (frame instanceof WebSocketFrame.Text(String payload) && deduplicating()
                && !recentMessages.add(payload, System.nanoTime())) {
            return;
        }
        listener.onMessage(session, frame);
    }

    private void rotate() {
        if (closed || active == null || standby != null) return;
        try {
            Session session = open();
            standby = session;
            updateViews();
            schedule(() -> {
                if (standby == session) {
                    log.warn("New session of {} not ready after {}, keeping the current one", options.connectionId().value(), rotation.readyTimeout());
                    abandonStandby();
                }
            }, rotation.readyTimeout());
        } catch (RuntimeException e) {
            log.warn("Cannot open new session of {}, retrying in {}", options.connectionId().value(), rotation.retryDelay(), e);
            rotationTask = schedule(this::rotate, rotation.retryDelay());
        }
    }

    private void promoteStandby() {
        Session old = active;
        active = standby;
        standby = null;
        if (old != null) {
            retire(old);
        }
        if (connected) {
            updateViews();
            scheduleRotation();
        } else {
            markConnected();
        }
    }

    /** Session cũ nhận nốt message đang trên đường trong drainDelay rồi mới đóng. */
    private void retire(Session old) {
        if (retiring != null) {
            retiring.client.close();
        }
        retiring = old;
        schedule(() -> {
            old.client.close();
            if (retiring == old) {
                retiring = null;
                dedupeUntil = System.nanoTime() + rotation.dedupeWindow().toNanos();
            }
        }, rotation.drainDelay());
    }

    private void abandonStandby() {
        standby.client.close();
        standby = null;
        dedupeUntil = System.nanoTime() + rotation.dedupeWindow().toNanos();
        updateViews();
        cancel(rotationTask);
        rotationTask = schedule(this::rotate, rotation.retryDelay());
    }

    private void markConnected() {
        connected = true;
        updateViews();
        scheduleRotation();
        listener.onConnected();
    }

    private void scheduleRotation() {
        cancel(rotationTask);
        rotationTask = schedule(this::rotate, rotation.interval());
    }

    /** Chỉ lọc trùng khi có hơn một session đang nhận, hoặc vừa đóng một session. */
    private boolean deduplicating() {
        if (standby != null || retiring != null) return true;
        if (dedupeUntil != null && System.nanoTime() - dedupeUntil < 0) return true;
        dedupeUntil = null;
        recentMessages.clear();
        return false;
    }

    private boolean isLive(Session session) {
        return session == active || session == standby || session == retiring;
    }

    private void updateViews() {
        openSessions = Stream.of(active, standby).filter(Objects::nonNull).map(RotatingSession.class::cast).toList();
        connectedView = connected;
    }

    private void execute(Runnable task) {
        try {
            control.execute(() -> runSafely(task));
        } catch (RejectedExecutionException e) {
            if (!closed) throw e;
        }
    }

    private ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        return control.schedule(() -> runSafely(task), delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("Rotating WebSocket {} task failed", options.connectionId().value(), e);
        }
    }

    private static void cancel(ScheduledFuture<?> task) {
        if (task != null) task.cancel(false);
    }

    /** Một session vật lý; nhận callback từ thread của WebSocketClient và chuyển sang thread control. */
    private final class Session implements RotatingSession, WebSocketClientListener {

        private WebSocketClient client;
        private boolean ready; // Chỉ truy cập trên thread control.

        @Override
        public CompletionStage<Void> send(WebSocketFrame frame) {
            return client.send(frame);
        }

        @Override
        public void markReady() {
            execute(() -> ready(this));
        }

        @Override
        public void onTransportConnected() {
            execute(() -> opened(this));
        }

        @Override
        public void onMessage(WebSocketFrame frame) {
            execute(() -> message(this, frame));
        }

        @Override
        public void onTransportDisconnected(CloseReason reason) {
            execute(() -> lost(this));
        }

        @Override
        public void onReconnecting(int attempt, int maxAttempts, Duration delay) {
            execute(() -> {
                if (this == active) listener.onReconnecting(attempt);
            });
        }

        @Override
        public void onError(TransportFailure failure) {
            execute(() -> {
                if (this == active || this == standby) listener.onError(failure);
            });
        }
    }
}

package com.hit.websocket;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Generic WebSocket connection manager with reconnect support.
 * Features:
 * - Connection lifecycle management
 * - Auto-reconnect with exponential backoff
 * - Heartbeat (with customizable ping handler)
 * - Thread-safe operations
 * 
 */
@Slf4j
public class WebSocketConnectionManager implements WebSocketHandler {

    private final WebSocketConfig config;
    private final WebSocketClient webSocketClient;

    /**
     * -- GETTER --
     *  Get current session (for advanced use cases)
     */
    // Connection state
    @Getter
    private WebSocketSession session;
    private volatile boolean isConnecting = false;
    private volatile boolean shouldReconnect = true;

    // Heartbeat
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledFuture<?> heartbeatTask;
    private volatile long lastPongTime = 0;

    // Reconnect
    private ScheduledExecutorService reconnectExecutor;
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    // Listener
    @Setter
    private WebSocketMessageListener messageListener;

    // Custom ping handler (optional - for protocol-specific ping)
    @Setter
    private Consumer<WebSocketSession> pingHandler;

    // Custom pong handler (optional - for protocol-specific pong notification)
    @Setter
    private Consumer<Void> pongHandler;

    // Lock for thread-safety
    private final ReentrantLock connectionLock = new ReentrantLock();

    public WebSocketConnectionManager(WebSocketConfig config) {
        if (Objects.isNull(config)) {
            throw new IllegalArgumentException("WebSocketConfig cannot be null");
        }
        config.validate(); // Validate all config values
        this.config = config;
        this.webSocketClient = new StandardWebSocketClient();
    }

    /**
     * Connect to WebSocket server
     */
    public void connect() {
        connectionLock.lock();
        try {
            if (isConnected()) {
                log.debug("Already connected, skipping connection attempt");
                return;
            }

            if (isConnecting) {
                log.debug("Connection in progress, skipping duplicate attempt");
                return;
            }

            isConnecting = true;
            shouldReconnect = true;
        } finally {
            connectionLock.unlock();
        }

        boolean connectionSuccessful = false;
        try {
            log.info("Connecting to WebSocket: {}", config.getUrl());
            session = webSocketClient.execute(this, config.getUrl())
                    .get(config.getConnectionTimeout(), TimeUnit.SECONDS);
            log.info("WebSocket transport connected");
            connectionSuccessful = true;

        } catch (Exception e) {
            log.error("Failed to connect to WebSocket", e);

            // Trigger reconnect if enabled
            if (config.isAutoReconnect()) {
                scheduleReconnect();
            } else {
                throw new RuntimeException("WebSocket connection failed", e);
            }
        } finally {
            connectionLock.lock();
            try {
                isConnecting = false;
                // Stop reconnect executor if connection was successful
                if (connectionSuccessful) {
                    stopReconnect();
                }
            } finally {
                connectionLock.unlock();
            }
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        connectionLock.lock();
        try {
            shouldReconnect = false;

            if (Objects.isNull(session)) {
                log.debug("Already disconnected, skipping");
                return;
            }

            log.info("Disconnecting WebSocket...");

            // Stop heartbeat and reconnect
            stopHeartbeat();
            stopReconnect();

            // Close session
            if (Objects.nonNull(session) && session.isOpen()) {
                try {
                    session.close();
                } catch (Exception e) {
                    log.warn("Error closing session", e);
                }
            }

            // Reset state
            session = null;
            isConnecting = false;
            reconnectAttempt.set(0);

            log.info("WebSocket disconnected");

        } catch (Exception e) {
            log.error("Error during disconnect", e);
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * Check if connected (transport level only)
     */
    public boolean isConnected() {
        return Objects.nonNull(session) && session.isOpen();
    }

    /**
     * Check connection health (including pong timeout if enabled)
     * Returns false if:
     * - Not connected
     * - Pong timeout exceeded (if pong tracking is enabled)
     */
    public boolean isHealthy() {
        if (!isConnected()) {
            return false;
        }

        // Check pong timeout (only if pong tracking is enabled)
        if (lastPongTime > 0) {
            long timeSincePong = System.currentTimeMillis() - lastPongTime;
            long maxPongDelay = config.getHeartbeatInterval() * 2;
            if (timeSincePong > maxPongDelay) {
                log.warn("No pong received for {}ms (max: {}ms)", timeSincePong, maxPongDelay);
                return false;
            }
        }

        return true;
    }

    /**
     * Mark that a pong was received (call this from protocol-specific pong handler)
     * This enables pong timeout tracking for health checks
     */
    public void markPongReceived() {
        lastPongTime = System.currentTimeMillis();
        
        if (Objects.nonNull(pongHandler)) {
            pongHandler.accept(null);
        }
    }

    /**
     * Send message through WebSocket
     */
    public void sendMessage(String message) {
        if (Objects.isNull(message)) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        if (!isConnected()) {
            throw new IllegalStateException("Not connected");
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            log.error("Failed to send message", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    // ========== WebSocketHandler Implementation ==========

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket transport connection established");

        connectionLock.lock();
        try {
            // Reset reconnect attempt counter on successful connection
            reconnectAttempt.set(0);
            // Stop reconnect executor to prevent memory leak
            stopReconnect();
        } finally {
            connectionLock.unlock();
        }

        // Reset pong time
        lastPongTime = 0;

        // Start heartbeat
        startHeartbeat();

        if (Objects.nonNull(messageListener)) {
            messageListener.onTransportConnected(session);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            String payload = message.getPayload().toString();

            if (Objects.nonNull(messageListener)) {
                messageListener.onMessage(payload);
            }

        } catch (Exception e) {
            log.error("Error handling message", e);
            if (Objects.nonNull(messageListener)) {
                messageListener.onError(e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error", exception);
        if (Objects.nonNull(messageListener)) {
            messageListener.onError(exception);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        log.info("WebSocket transport connection closed: {}", closeStatus);

        connectionLock.lock();
        try {
            this.session = null;
            this.lastPongTime = 0;
            stopHeartbeat();
        } finally {
            connectionLock.unlock();
        }

        if (Objects.nonNull(messageListener)) {
            messageListener.onTransportDisconnected(closeStatus);
        }

        // Auto-reconnect if enabled and not normal closure
        if (config.isAutoReconnect() && shouldReconnect && closeStatus.getCode() != 1000) {
            scheduleReconnect();
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // ========== Private Methods ==========

    private void startHeartbeat() {
        if (Objects.nonNull(heartbeatExecutor)) {
            stopHeartbeat();
        }

        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "websocket-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (Objects.isNull(session) || !session.isOpen()) {
                    return;
                }

                // Check health (including pong timeout)
                if (!isHealthy()) {
                    log.error("Health check failed, closing connection to trigger reconnect");
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.warn("Error closing unhealthy session", e);
                    }
                    return;
                }

                // Send ping if handler is configured
                if (Objects.nonNull(pingHandler)) {
                    pingHandler.accept(session);
                    log.debug("Heartbeat ping sent (custom handler)");
                } else {
                    log.trace("Heartbeat tick (no ping handler configured)");
                }

            } catch (Exception e) {
                log.error("Failed to send heartbeat", e);
            }
        }, config.getHeartbeatInterval(), config.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (Objects.nonNull(heartbeatTask)) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }
        shutdownExecutor(heartbeatExecutor);
        heartbeatExecutor = null;
    }

    private void scheduleReconnect() {
        int currentAttempt = reconnectAttempt.get();
        
        // Check if max attempts reached (only if maxReconnectAttempts > 0)
        if (config.getMaxReconnectAttempts() > 0 && currentAttempt >= config.getMaxReconnectAttempts()) {
            log.error("Max reconnect attempts ({}) reached, giving up", config.getMaxReconnectAttempts());
            return;
        }

        int nextAttempt = reconnectAttempt.incrementAndGet();

        // Exponential backoff with jitter
        long delay = config.getInitialReconnectDelay() * (long) Math.pow(2, nextAttempt - 1);
        delay = Math.min(delay, config.getMaxReconnectDelay());

        // Add jitter (±20%)
        long jitter = (long) (delay * 0.2 * Math.random());
        delay = delay + jitter;

        if (config.getMaxReconnectAttempts() > 0) {
            log.info("Scheduling reconnect attempt {}/{} in {}ms",
                    nextAttempt, config.getMaxReconnectAttempts(), delay);
        } else {
            log.info("Scheduling reconnect attempt {} (unlimited) in {}ms", nextAttempt, delay);
        }

        if (Objects.nonNull(messageListener)) {
            messageListener.onReconnecting(nextAttempt, config.getMaxReconnectAttempts());
        }

        if (Objects.isNull(reconnectExecutor)) {
            reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "websocket-reconnect");
                thread.setDaemon(true);
                return thread;
            });
        }

        reconnectExecutor.schedule(() -> {
            if (config.getMaxReconnectAttempts() > 0) {
                log.info("Reconnecting... (attempt {}/{})", nextAttempt, config.getMaxReconnectAttempts());
            } else {
                log.info("Reconnecting... (attempt {}, unlimited)", nextAttempt);
            }
            connect();
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void stopReconnect() {
        reconnectAttempt.set(0);
        if (Objects.nonNull(reconnectExecutor)) {
            shutdownExecutor(reconnectExecutor);
            reconnectExecutor = null;
        }
    }

    /**
     * Shutdown executor gracefully
     */
    private void shutdownExecutor(ExecutorService executor) {
        if (Objects.nonNull(executor) && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate gracefully, forcing shutdown");
                    executor.shutdownNow();
                    // Wait a bit more for tasks to respond to being cancelled
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        log.error("Executor did not terminate after forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for executor termination");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

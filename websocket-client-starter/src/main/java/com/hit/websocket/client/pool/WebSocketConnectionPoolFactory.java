package com.hit.websocket.client.pool;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.IntFunction;

/** Creates connection pools with a shared library-managed thread policy. */
public final class WebSocketConnectionPoolFactory {

    private static final String DEFAULT_THREAD_NAME = "websocket-pool-";

    private final ThreadFactory threads;

    public WebSocketConnectionPoolFactory() {
        this(Thread.ofPlatform().daemon().name(DEFAULT_THREAD_NAME, 0).factory());
    }

    public WebSocketConnectionPoolFactory(ThreadFactory threads) {
        this.threads = Objects.requireNonNull(threads, "threads cannot be null");
    }

    public <R, C extends PooledWebSocketConnection> WebSocketConnectionPool<R, C> create(
            WebSocketConnectionPoolOptions options, IntFunction<C> connectionFactory) {
        return new WebSocketConnectionPool<>(options, connectionFactory, threads);
    }
}

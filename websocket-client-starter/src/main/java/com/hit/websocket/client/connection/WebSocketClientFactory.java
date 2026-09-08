package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.WebSocketTransportFactory;
import com.hit.websocket.client.observability.WebSocketObserver;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Owns created clients and closes their control threads when the application stops. */
public final class WebSocketClientFactory implements AutoCloseable {
    private final WebSocketTransportFactory transportFactory;
    private final List<WebSocketObserver> observers;
    private final Clock clock;
    private final Map<ConnectionId, WebSocketClient> clients = new HashMap<>();
    private final AtomicLong generations = new AtomicLong();
    private boolean closed;

    public WebSocketClientFactory(WebSocketTransportFactory transportFactory,
                                         List<WebSocketObserver> observers, Clock clock) {
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.observers = List.copyOf(observers);
        this.clock = Objects.requireNonNull(clock);
    }

    // Only construction/shutdown share a lock. Connection traffic never takes this lock.
    public synchronized WebSocketClient create(WebSocketClientOptions options, WebSocketClientListener listener) {
        if (closed) throw new IllegalStateException("WebSocket client factory is closed");
        if (clients.containsKey(options.connectionId())) throw new IllegalArgumentException("Connection id is already in use: " + options.connectionId());
        WebSocketClient client = new DefaultWebSocketClient(options, listener, this);
        clients.put(options.connectionId(), client);
        return client;
    }

    @Override
    public synchronized void close() {
        closed = true;
        clients.values().forEach(WebSocketClient::close);
        clients.clear();
    }

    WebSocketTransportFactory transportFactory() { return transportFactory; }
    List<WebSocketObserver> observers() { return observers; }
    Clock clock() { return clock; }
    long nextGeneration() { return generations.incrementAndGet(); }

    synchronized void release(ConnectionId id, WebSocketClient client) {
        clients.remove(id, client);
    }
}

package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.WebSocketTransportFactory;
import com.hit.websocket.client.observability.ConnectionObserver;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public final class WebSocketClientManagerFactory {

    private final WebSocketTransportFactory transportFactory;
    private final ScheduledExecutorService scheduler;
    private final List<ConnectionObserver> observers;
    private final Clock clock;

    public WebSocketClientManagerFactory(WebSocketTransportFactory transportFactory, ScheduledExecutorService scheduler,
                                         List<ConnectionObserver> observers) {
        this(transportFactory, scheduler, observers, Clock.systemUTC());
    }

    WebSocketClientManagerFactory(WebSocketTransportFactory transportFactory, ScheduledExecutorService scheduler,
                                  List<ConnectionObserver> observers, Clock clock) {
        this.transportFactory = transportFactory;
        this.scheduler = scheduler;
        this.observers = List.copyOf(observers);
        this.clock = clock;
    }

    public ManagedWebSocketClient create(WebSocketClientOptions options, WebSocketClientListener listener) {
        return new WebSocketClientManager(options, listener, transportFactory, scheduler, observers, clock);
    }
}

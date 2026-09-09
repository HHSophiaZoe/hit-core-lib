package com.hit.websocket.client.dispatch;

import com.hit.websocket.client.connection.ConnectionId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageDispatcherRegistry {

    private final MessageDispatcherFactory dispatcherFactory;
    private final MessageDispatcherOptionsProvider optionsProvider;
    private final Map<DispatcherId, MessageDispatcher> dispatchers = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public MessageDispatcherRegistry(MessageDispatcherFactory dispatcherFactory, MessageDispatcherOptionsProvider optionsProvider) {
        this.dispatcherFactory = dispatcherFactory;
        this.optionsProvider = optionsProvider;
    }

    public MessageDispatcher get(ConnectionId connectionId, String dispatcherName) {
        if (closed.get()) throw new IllegalStateException("Message dispatcher registry is closed");
        DispatcherId id = new DispatcherId(connectionId, dispatcherName);
        MessageDispatcher dispatcher = dispatchers.computeIfAbsent(id, this::createDispatcher);
        if (!closed.get()) return dispatcher;
        if (dispatchers.remove(id, dispatcher)) dispatcher.close();
        throw new IllegalStateException("Message dispatcher registry is closed");
    }

    private MessageDispatcher createDispatcher(DispatcherId id) {
        if (closed.get()) throw new IllegalStateException("Message dispatcher registry is closed");
        MessageDispatcherOptions options = optionsProvider.getOptions(id.name());
        return dispatcherFactory.create(id.connectionId(), options);
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        dispatchers.values().forEach(MessageDispatcher::close);
        dispatchers.clear();
    }

    private record DispatcherId(ConnectionId connectionId, String name) {
    }
}

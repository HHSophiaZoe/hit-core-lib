package com.hit.websocket.client.dispatch;

import java.util.Map;
import java.util.HashMap;

public class MessageDispatcherRegistry {

    private final MessageDispatcherFactory dispatcherFactory;
    private final MessageDispatcherOptionsProvider optionsProvider;
    private final Map<String, MessageDispatcher> dispatchers = new HashMap<>();
    private boolean closed;

    public MessageDispatcherRegistry(MessageDispatcherFactory dispatcherFactory, MessageDispatcherOptionsProvider optionsProvider) {
        this.dispatcherFactory = dispatcherFactory;
        this.optionsProvider = optionsProvider;
    }

    public synchronized MessageDispatcher get(String instanceName) {
        if (closed) throw new IllegalStateException("Message dispatcher registry is closed");
        return dispatchers.computeIfAbsent(instanceName, this::createDispatcher);
    }

    private MessageDispatcher createDispatcher(String dispatcherName) {
        MessageDispatcherOptions options = optionsProvider.getOptions(dispatcherName);
        return dispatcherFactory.create(options);
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        dispatchers.values().forEach(MessageDispatcher::close);
        dispatchers.clear();
    }
}

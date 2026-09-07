package com.hit.websocket.client.dispatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDispatcherRegistry {

    private final MessageDispatcherFactory dispatcherFactory;
    private final MessageDispatcherOptionsProvider optionsProvider;
    private final Map<String, MessageDispatcher> dispatchers = new ConcurrentHashMap<>();

    public MessageDispatcherRegistry(MessageDispatcherFactory dispatcherFactory, MessageDispatcherOptionsProvider optionsProvider) {
        this.dispatcherFactory = dispatcherFactory;
        this.optionsProvider = optionsProvider;
    }

    public MessageDispatcher get(String instanceName) {
        return dispatchers.computeIfAbsent(instanceName, this::createDispatcher);
    }

    private MessageDispatcher createDispatcher(String dispatcherName) {
        MessageDispatcherOptions options = optionsProvider.getOptions(dispatcherName);
        return dispatcherFactory.create(options);
    }

    public void close() {
        dispatchers.values().forEach(MessageDispatcher::close);
        dispatchers.clear();
    }
}

package com.hit.websocket.client.dispatch;

import java.util.List;

public class MessageDispatcherFactory {

    private final List<MessageDispatcherObserver> observers;

    public MessageDispatcherFactory(List<MessageDispatcherObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    public MessageDispatcher create(MessageDispatcherOptions options) {
        return new PartitionedMessageDispatcher(options, observers);
    }
}

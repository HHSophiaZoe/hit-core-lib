package com.hit.websocket.client.dispatch;

import com.hit.websocket.client.connection.ConnectionId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

public class MessageDispatcherFactory {

    private final List<MessageDispatcherObserver> observers;
    private final ThreadFactory threads;

    public MessageDispatcherFactory(List<MessageDispatcherObserver> observers) {
        this(observers, Thread.ofPlatform().daemon().name("websocket-dispatcher-", 0).factory());
    }

    public MessageDispatcherFactory(List<MessageDispatcherObserver> observers, ThreadFactory threads) {
        this.observers = List.copyOf(observers);
        this.threads = Objects.requireNonNull(threads, "threads");
    }

    public MessageDispatcher create(ConnectionId connectionId, MessageDispatcherOptions options) {
        return new PartitionedMessageDispatcher(connectionId, options, observers, threads);
    }
}

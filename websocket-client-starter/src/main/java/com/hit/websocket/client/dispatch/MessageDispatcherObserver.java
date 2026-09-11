package com.hit.websocket.client.dispatch;

import com.hit.websocket.client.connection.ConnectionId;
import java.time.Duration;
import java.util.function.IntSupplier;

public interface MessageDispatcherObserver {

    default void queueRegistered(ConnectionId connectionId, String dispatcher, int partition, IntSupplier queueSize) {
    }

    default void processed(DispatchContext context, Duration duration) {
    }

    default void dropped(DispatchContext context) {
    }

    default void failed(DispatchContext context, Throwable error) {
    }
}

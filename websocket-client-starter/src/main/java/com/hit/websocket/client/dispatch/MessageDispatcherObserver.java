package com.hit.websocket.client.dispatch;

import java.time.Duration;
import java.util.function.IntSupplier;

public interface MessageDispatcherObserver {

    default void queueRegistered(String dispatcher, int partition, IntSupplier queueSize) {
    }

    default void processed(DispatchContext context, Duration duration) {
    }

    default void dropped(DispatchContext context) {
    }

    default void failed(DispatchContext context, Throwable error) {
    }
}

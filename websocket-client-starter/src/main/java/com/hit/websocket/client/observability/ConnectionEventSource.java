package com.hit.websocket.client.observability;

import com.hit.websocket.client.observability.model.ConnectionEvent;

import java.util.function.Consumer;

public interface ConnectionEventSource {

    Subscription subscribe(Consumer<ConnectionEvent> consumer);

    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}

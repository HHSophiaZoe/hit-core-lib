package com.hit.websocket.client.observability;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.observability.model.ConnectionEvent;

public interface WebSocketObserver {

    default void onEvent(ConnectionEvent event) {
    }

    default void onMessageReceived(ConnectionId connectionId, long generation, int payloadBytes) {
    }

    default void onMessageSent(ConnectionId connectionId, long generation, int payloadBytes) {
    }
}

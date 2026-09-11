package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketFrame;

import java.time.Duration;

public interface WebSocketClientListener {

    default void onTransportConnected() {
    }

    void onMessage(WebSocketFrame frame);

    /** Transport acceptance, not a peer acknowledgement. Includes automatic heartbeat frames. */
    default void onSendCompleted(WebSocketFrame frame, boolean accepted) {
    }

    default void onTransportDisconnected(CloseReason reason) {
    }

    default void onError(TransportFailure failure) {
    }

    default void onReconnecting(int attempt, int maxAttempts, Duration delay) {
    }
}

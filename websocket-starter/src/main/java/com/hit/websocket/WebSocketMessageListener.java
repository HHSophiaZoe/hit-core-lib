package com.hit.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * Callback interface for WebSocket transport events.
 */
public interface WebSocketMessageListener {

    /**
     * Called when WebSocket transport connection is established
     * (NOT authenticated yet - business layer should handle authentication)
     * @param session WebSocket session
     */
    void onTransportConnected(WebSocketSession session);

    /**
     * Called when a raw message is received from WebSocket
     * @param message Raw message string
     */
    void onMessage(String message);

    /**
     * Called when WebSocket transport connection is closed
     * @param closeStatus Close status from WebSocket
     */
    void onTransportDisconnected(CloseStatus closeStatus);

    /**
     * Called when a transport error occurs
     * @param error The error that occurred
     */
    void onError(Throwable error);

    /**
     * Called when reconnection is in progress
     * @param attempt Current attempt number
     * @param maxRetries Maximum retry attempts
     */
    default void onReconnecting(int attempt, int maxRetries) {
        // Default empty implementation
    }
}

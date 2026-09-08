package com.hit.websocket.client.connection;

public enum ConnectionState {
    STOPPED,
    CONNECTING,
    CONNECTED,
    RETRY_WAIT,
    DISCONNECTING,
    FAILED
}

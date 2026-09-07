package com.hit.websocket.client.connection;

public enum ConnectionState {
    STOPPED,
    CONNECTING,
    TRANSPORT_CONNECTED,
    AUTHENTICATING,
    RESUBSCRIBING,
    READY,
    RETRY_WAIT,
    DISCONNECTING,
    FAILED
}

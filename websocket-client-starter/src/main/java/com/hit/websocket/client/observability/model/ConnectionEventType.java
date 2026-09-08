package com.hit.websocket.client.observability.model;

public enum ConnectionEventType {
    CONNECT_REQUESTED,
    TRANSPORT_CONNECTED,
    RETRY_SCHEDULED,
    HEARTBEAT_TIMEOUT,
    TRANSPORT_CLOSED,
    ERROR,
    FAILED,
    DISCONNECT_REQUESTED,
    STOPPED
}

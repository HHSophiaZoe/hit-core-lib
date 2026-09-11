package com.hit.websocket.client.transport;

public enum FailureCategory {
    DNS,
    CONNECT_TIMEOUT,
    TLS,
    HANDSHAKE,
    NETWORK,
    PROTOCOL,
    AUTHENTICATION,
    SUBSCRIPTION,
    RATE_LIMIT,
    SERVER,
    SERIALIZATION,
    UNKNOWN
}

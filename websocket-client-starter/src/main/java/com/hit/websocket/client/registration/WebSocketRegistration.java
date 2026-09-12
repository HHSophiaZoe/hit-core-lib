package com.hit.websocket.client.registration;

import com.hit.websocket.client.connection.ConnectionId;

import java.util.Set;

/** Lifecycle handle returned for one logical WebSocket registration. */
public interface WebSocketRegistration {

    /** Physical connections currently serving this registration, or an empty set when not applicable. */
    Set<ConnectionId> connectionIds();

    /** Whether this registration has already been closed. */
    boolean isClosed();

    /** Closes this registration. Repeated calls have no effect. */
    void close();
}

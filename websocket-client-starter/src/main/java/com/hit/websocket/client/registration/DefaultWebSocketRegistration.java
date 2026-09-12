package com.hit.websocket.client.registration;

import com.hit.websocket.client.connection.ConnectionId;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Default registration implementation for both single-connection and pooled WebSocket clients. */
public final class DefaultWebSocketRegistration implements WebSocketRegistration {

    private final Runnable closeAction;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<Set<ConnectionId>> connectionIds;

    /** Creates a registration whose connection assignment is not known yet or is not exposed by the client. */
    public DefaultWebSocketRegistration(Runnable closeAction) {
        this(Set.of(), closeAction);
    }

    /** Creates a registration served by one physical connection. */
    public DefaultWebSocketRegistration(ConnectionId connectionId, Runnable closeAction) {
        this(Set.of(Objects.requireNonNull(connectionId, "connectionId cannot be null")), closeAction);
    }

    /** Creates a registration served by the supplied physical connections. */
    public DefaultWebSocketRegistration(Collection<ConnectionId> connectionIds, Runnable closeAction) {
        this.connectionIds = new AtomicReference<>(immutableConnections(connectionIds));
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction cannot be null");
    }

    /** Replaces the connection assignment, for example after allocation by a connection pool. */
    public void assignConnections(Collection<ConnectionId> connections) {
        connectionIds.set(immutableConnections(connections));
    }

    @Override
    public Set<ConnectionId> connectionIds() {
        return connectionIds.get();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) closeAction.run();
    }

    private static Set<ConnectionId> immutableConnections(Collection<ConnectionId> connections) {
        return Set.copyOf(Objects.requireNonNull(connections, "connections cannot be null"));
    }
}

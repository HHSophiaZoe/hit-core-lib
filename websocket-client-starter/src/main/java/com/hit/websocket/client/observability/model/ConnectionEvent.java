package com.hit.websocket.client.observability.model;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.connection.ConnectionState;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

/** Lifecycle event safe to expose to monitoring consumers. Attributes must never contain credentials. */
@Builder
public record ConnectionEvent(
        ConnectionId connectionId,
        long generation,
        long sequence,
        ConnectionEventType type,
        ConnectionState state,
        Instant occurredAt,
        ConnectionEventDetails details,
        Map<String, String> metadata
) {
    public ConnectionEvent {
        Objects.requireNonNull(connectionId, "connectionId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        details = details == null ? ConnectionEventDetails.None.INSTANCE : details;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public ConnectionEvent(
            ConnectionId connectionId, long generation, long sequence, ConnectionEventType type,
            ConnectionState state, Instant occurredAt, ConnectionEventDetails details
    ) {
        this(connectionId, generation, sequence, type, state, occurredAt, details, Map.of());
    }
}

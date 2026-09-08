package com.hit.websocket.client.observability.model;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.connection.ConnectionState;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ConnectionSnapshot(
        ConnectionId connectionId,
        long generation,
        long lastSequence,
        ConnectionState state,
        Instant stateChangedAt,
        Instant connectedAt,
        Instant lastMessageAt,
        Instant lastErrorAt,
        String lastErrorCategory,
        String lastErrorCode,
        String lastErrorMessage,
        long reconnectAttempts,
        long receivedMessages,
        long receivedBytes,
        long sentMessages,
        long sentBytes,
        ConnectionDailySnapshot today
) {
}

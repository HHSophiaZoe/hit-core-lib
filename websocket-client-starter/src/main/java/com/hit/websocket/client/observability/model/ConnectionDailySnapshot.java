package com.hit.websocket.client.observability.model;

import lombok.Builder;

import java.time.LocalDate;

/** Process-local counters for one configured calendar day. Durable history belongs to a metrics backend. */
@Builder
public record ConnectionDailySnapshot(
        LocalDate date,
        long connectAttempts,
        long transportConnections,
        long readyTransitions,
        long disconnects,
        long connectionLosses,
        long retries,
        long errors,
        long heartbeatTimeouts,
        long receivedMessages,
        long receivedBytes,
        long sentMessages,
        long sentBytes
) {
}

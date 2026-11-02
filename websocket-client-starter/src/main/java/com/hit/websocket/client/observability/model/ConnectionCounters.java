package com.hit.websocket.client.observability.model;

import lombok.Builder;

/**
 * Process-local counters of one connection since the registry started. They only grow, so a consumer gets the
 * activity of any window by subtracting two snapshots; durable history belongs to the consumer.
 */
@Builder
public record ConnectionCounters(
        long connectAttempts,
        long transportConnections,
        long disconnects, // Disconnects requested by the client.
        long connectionLosses, // Unrequested closes and terminal errors after the connection was established.
        long retries,
        long errors,
        long heartbeatTimeouts,
        long receivedMessages,
        long receivedBytes,
        long sentMessages,
        long sentBytes
) {

    public static final ConnectionCounters ZERO = ConnectionCounters.builder().build();

    public ConnectionCounters plus(ConnectionCounters other) {
        return ConnectionCounters.builder()
                .connectAttempts(connectAttempts + other.connectAttempts)
                .transportConnections(transportConnections + other.transportConnections)
                .disconnects(disconnects + other.disconnects)
                .connectionLosses(connectionLosses + other.connectionLosses)
                .retries(retries + other.retries)
                .errors(errors + other.errors)
                .heartbeatTimeouts(heartbeatTimeouts + other.heartbeatTimeouts)
                .receivedMessages(receivedMessages + other.receivedMessages)
                .receivedBytes(receivedBytes + other.receivedBytes)
                .sentMessages(sentMessages + other.sentMessages)
                .sentBytes(sentBytes + other.sentBytes)
                .build();
    }

    /** Activity between an earlier snapshot ({@code other}) and this one. */
    public ConnectionCounters minus(ConnectionCounters other) {
        return ConnectionCounters.builder()
                .connectAttempts(connectAttempts - other.connectAttempts)
                .transportConnections(transportConnections - other.transportConnections)
                .disconnects(disconnects - other.disconnects)
                .connectionLosses(connectionLosses - other.connectionLosses)
                .retries(retries - other.retries)
                .errors(errors - other.errors)
                .heartbeatTimeouts(heartbeatTimeouts - other.heartbeatTimeouts)
                .receivedMessages(receivedMessages - other.receivedMessages)
                .receivedBytes(receivedBytes - other.receivedBytes)
                .sentMessages(sentMessages - other.sentMessages)
                .sentBytes(sentBytes - other.sentBytes)
                .build();
    }
}

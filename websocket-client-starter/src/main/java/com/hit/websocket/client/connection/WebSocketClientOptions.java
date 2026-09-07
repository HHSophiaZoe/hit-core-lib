package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.WebSocketConnectRequest;
import lombok.Builder;

import java.time.Duration;
import java.util.Objects;

@Builder
public record WebSocketClientOptions(
        ConnectionId connectionId,
        WebSocketConnectRequest connectRequest,
        RetryPolicy retryPolicy,
        Duration heartbeatInterval,
        Duration pongTimeout,
        Duration lifecycleStageTimeout
) {
    public WebSocketClientOptions {
        Objects.requireNonNull(connectionId, "connectionId cannot be null");
        Objects.requireNonNull(connectRequest, "connectRequest cannot be null");
        Objects.requireNonNull(retryPolicy, "retryPolicy cannot be null");
        if (heartbeatInterval != null && (heartbeatInterval.isZero() || heartbeatInterval.isNegative())) {
            throw new IllegalArgumentException("heartbeatInterval must be positive when configured");
        }
        if (heartbeatInterval != null) {
            Objects.requireNonNull(pongTimeout, "pongTimeout is required when heartbeat is enabled");
            if (pongTimeout.isZero() || pongTimeout.isNegative()) {
                throw new IllegalArgumentException("pongTimeout must be positive");
            }
        }
        if (lifecycleStageTimeout != null && (lifecycleStageTimeout.isZero() || lifecycleStageTimeout.isNegative())) {
            throw new IllegalArgumentException("lifecycleStageTimeout must be positive when configured");
        }
    }
}

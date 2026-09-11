package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.WebSocketConnectRequest;
import lombok.Builder;

import java.time.Duration;
import java.util.Objects;

@Builder
public record WebSocketClientOptions(
        ConnectionId connectionId,
        WebSocketConnectRequest connectRequest,
        ReconnectPolicy reconnectPolicy,
        HeartbeatOptions heartbeat,
        int inboundCapacity
) {
    public WebSocketClientOptions {
        Objects.requireNonNull(connectionId, "connectionId cannot be null");
        Objects.requireNonNull(connectRequest, "connectRequest cannot be null");
        reconnectPolicy = reconnectPolicy == null ? ReconnectPolicy.unlimited(Duration.ofSeconds(1), Duration.ofSeconds(30)) : reconnectPolicy;
        inboundCapacity = inboundCapacity == 0 ? 1024 : inboundCapacity;
        if (inboundCapacity < 1) throw new IllegalArgumentException("inboundCapacity must be positive");
    }
}

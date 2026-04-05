package com.hit.websocket;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

/**
 * Configuration for WebSocket connection
 */
@Getter
@Builder
public class WebSocketConfig {

    /**
     * WebSocket URL
     */
    private final String url;

    /**
     * Heartbeat interval in milliseconds
     * Default: 25000 (25 seconds)
     */
    @Builder.Default
    private final long heartbeatInterval = 25000;

    /**
     * Enable auto-reconnect
     * Default: true
     */
    @Builder.Default
    private final boolean autoReconnect = true;

    /**
     * Maximum reconnect attempts
     * Default: -1 (unlimited, reconnect forever)
     * Set to 0 to disable reconnect
     * Set to positive number for limited attempts
     */
    @Builder.Default
    private final int maxReconnectAttempts = -1;

    /**
     * Initial reconnect delay in milliseconds
     * Default: 1000 (1 second)
     */
    @Builder.Default
    private final long initialReconnectDelay = 1000;

    /**
     * Maximum reconnect delay in milliseconds
     * Default: 60000 (60 seconds)
     */
    @Builder.Default
    private final long maxReconnectDelay = 30000;

    /**
     * Connection timeout in seconds
     * Default: 10
     */
    @Builder.Default
    private final int connectionTimeout = 10;

    /**
     * Validate configuration values
     */
    public void validate() {
        if (Objects.isNull(url) || url.trim().isEmpty()) {
            throw new IllegalArgumentException("WebSocket URL cannot be null or empty");
        }
        if (heartbeatInterval <= 0) {
            throw new IllegalArgumentException("Heartbeat interval must be positive");
        }
        if (initialReconnectDelay < 0) {
            throw new IllegalArgumentException("Initial reconnect delay cannot be negative");
        }
        if (maxReconnectDelay < initialReconnectDelay) {
            throw new IllegalArgumentException("Max reconnect delay must be >= initial reconnect delay");
        }
        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
    }
}

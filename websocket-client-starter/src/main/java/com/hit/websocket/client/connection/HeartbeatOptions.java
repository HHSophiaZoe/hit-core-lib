package com.hit.websocket.client.connection;

import java.time.Duration;
import java.util.Objects;

/** Null heartbeat options disable heartbeat; configured options use native Ping/Pong by default. */
public record HeartbeatOptions(Duration interval, Duration pongTimeout) {
    public HeartbeatOptions {
        positive(interval, "interval");
        positive(pongTimeout, "pongTimeout");
    }

    private static void positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || value.isZero()) throw new IllegalArgumentException(name + " must be positive");
        value.toNanos(); // Reject values that cannot be scheduled.
    }
}

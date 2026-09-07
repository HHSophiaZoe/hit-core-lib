package com.hit.websocket.client.dispatch;

import java.util.Objects;

public record MessageDispatcherOptions(
        String name,
        int partitions,
        /** Capacity of each partition queue, not the combined dispatcher capacity. */
        int queueCapacity,
        DispatchOverflowPolicy overflowPolicy
) {
    public MessageDispatcherOptions {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        if (partitions <= 0) throw new IllegalArgumentException("partitions must be positive");
        if (queueCapacity <= 0) throw new IllegalArgumentException("queueCapacity must be positive");
        Objects.requireNonNull(overflowPolicy, "overflowPolicy cannot be null");
    }
}

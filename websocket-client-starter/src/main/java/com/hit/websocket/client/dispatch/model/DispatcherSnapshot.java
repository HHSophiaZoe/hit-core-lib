package com.hit.websocket.client.dispatch.model;

import com.hit.websocket.client.connection.ConnectionId;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record DispatcherSnapshot(
        ConnectionId connectionId,
        String name,
        int queued,
        long processed,
        long dropped,
        long errors,
        long totalProcessingNanos,
        Instant lastProcessedAt,
        Instant lastErrorAt,
        List<DispatcherPartitionSnapshot> partitions
) {
}

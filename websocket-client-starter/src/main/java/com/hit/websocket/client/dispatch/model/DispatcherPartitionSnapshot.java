package com.hit.websocket.client.dispatch.model;

import lombok.Builder;

@Builder
public record DispatcherPartitionSnapshot(int partition, int queued, long processed, long dropped, long errors) {
}

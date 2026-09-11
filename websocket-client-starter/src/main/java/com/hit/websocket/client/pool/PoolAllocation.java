package com.hit.websocket.client.pool;

import com.hit.websocket.client.connection.ConnectionId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PoolAllocation<R>(
        Map<ConnectionId, List<R>> assigned,
        Map<ConnectionId, List<R>> added
) {
    public PoolAllocation {
        assigned = immutableCopy(assigned);
        added = immutableCopy(added);
    }

    public Set<ConnectionId> connectionIds() {
        return assigned.keySet();
    }

    private static <R> Map<ConnectionId, List<R>> immutableCopy(Map<ConnectionId, List<R>> source) {
        Map<ConnectionId, List<R>> copy = new LinkedHashMap<>();
        source.forEach((connectionId, resources) -> copy.put(connectionId, List.copyOf(resources)));
        return Map.copyOf(copy);
    }
}

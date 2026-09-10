package com.hit.websocket.client.pool;

import com.hit.websocket.client.connection.ConnectionId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PoolRelease<R>(
        Map<ConnectionId, List<R>> removed,
        Set<ConnectionId> unusedConnections
) {
    public PoolRelease {
        Map<ConnectionId, List<R>> copy = new LinkedHashMap<>();
        removed.forEach((connectionId, resources) -> copy.put(connectionId, List.copyOf(resources)));
        removed = Map.copyOf(copy);
        unusedConnections = Set.copyOf(unusedConnections);
    }
}

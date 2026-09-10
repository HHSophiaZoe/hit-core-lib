package com.hit.websocket.client.observability;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.observability.model.ConnectionSnapshot;

import java.util.List;
import java.util.Optional;

public interface ConnectionSnapshotQuery {

    Optional<ConnectionSnapshot> find(ConnectionId connectionId);

    List<ConnectionSnapshot> findAll();

    default List<ConnectionSnapshot> findBySource(String source) {
        return findAll().stream()
                .filter(snapshot -> snapshot.connectionId().provider().equals(source))
                .toList();
    }
}

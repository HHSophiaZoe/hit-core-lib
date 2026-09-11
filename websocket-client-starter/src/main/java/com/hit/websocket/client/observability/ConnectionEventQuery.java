package com.hit.websocket.client.observability;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.observability.model.ConnectionEvent;

import java.util.List;

public interface ConnectionEventQuery {
    List<ConnectionEvent> findRecent(ConnectionId connectionId, int limit);
    List<ConnectionEvent> findRecent(int limit);

    default List<ConnectionEvent> findRecentBySource(String source, int limit) {
        return findRecent(limit).stream()
                .filter(event -> event.connectionId().provider().equals(source))
                .limit(limit)
                .toList();
    }
}

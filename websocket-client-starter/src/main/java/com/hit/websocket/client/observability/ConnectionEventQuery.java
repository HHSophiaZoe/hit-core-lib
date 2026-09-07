package com.hit.websocket.client.observability;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.observability.model.ConnectionEvent;

import java.util.List;

public interface ConnectionEventQuery {
    List<ConnectionEvent> findRecent(ConnectionId connectionId, int limit);
    List<ConnectionEvent> findRecent(int limit);
}

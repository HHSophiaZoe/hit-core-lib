package com.hit.websocket.client.dispatch;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.dispatch.model.DispatcherSnapshot;

import java.util.List;

public interface DispatcherSnapshotQuery {

    List<DispatcherSnapshot> findByConnection(ConnectionId connectionId);
}

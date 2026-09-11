package com.hit.websocket.client.transport;

import com.hit.websocket.client.connection.ConnectionId;

public interface WebSocketTransportFactory {

    WebSocketTransport create(ConnectionId connectionId);
}

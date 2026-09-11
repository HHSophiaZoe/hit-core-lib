package com.hit.websocket.client.pool;

import com.hit.websocket.client.connection.ConnectionId;

/** Physical connection managed by a {@link WebSocketConnectionPool}. */
public interface PooledWebSocketConnection {

    ConnectionId connectionId();

    void connect();

    void disconnect();

    boolean isConnected();

    void maintain();

    void close();
}

package com.hit.websocket.client.transport.reactor;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.transport.WebSocketTransport;
import com.hit.websocket.client.transport.WebSocketTransportFactory;
import reactor.netty.http.client.HttpClient;

import java.util.Objects;

public final class ReactorNettyWebSocketTransportFactory implements WebSocketTransportFactory {

    private final HttpClient httpClient;

    public ReactorNettyWebSocketTransportFactory(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public WebSocketTransport create(ConnectionId connectionId) {
        return new ReactorNettyWebSocketTransport(httpClient, connectionId);
    }
}

package com.hit.websocket.client.transport;

public interface WebSocketTransportListener {

    void onFrame(WebSocketFrame frame);

    void onClosed(CloseReason reason);

    void onFailure(TransportFailure failure);
}

package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.WebSocketFrame;

import java.util.concurrent.CompletionStage;

/** Một session vật lý của RotatingWebSocketClient. */
public interface RotatingSession {

    CompletionStage<Void> send(WebSocketFrame frame);

    /** Adapter gọi khi session đã đăng ký/xác thực xong theo protocol của provider và nhận được dữ liệu. */
    void markReady();
}

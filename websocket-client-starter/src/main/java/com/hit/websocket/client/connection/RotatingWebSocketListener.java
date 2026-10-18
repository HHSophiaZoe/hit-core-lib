package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketFrame;

/** Callback của RotatingWebSocketClient; luôn được gọi tuần tự trên một thread, không được chặn lâu. */
public interface RotatingWebSocketListener {

    /** Session vừa kết nối (lần đầu, kết nối lại, hoặc session mới khi xoay): gửi đăng ký/xác thực rồi gọi markReady(). */
    void onSessionOpened(RotatingSession session);

    /** Message đã lọc trùng, kèm session nhận được để adapter khớp phản hồi lệnh theo session. */
    void onMessage(RotatingSession session, WebSocketFrame frame);

    /** Kết nối sẵn sàng lần đầu hoặc sau khi bị ngắt; không gọi khi xoay session theo kế hoạch. */
    default void onConnected() {
    }

    /** Session đang dùng mất kết nối ngoài kế hoạch; dữ liệu trong khoảng ngắt có thể bị thiếu. */
    default void onDisconnected() {
    }

    /** Session đang dùng đang thử kết nối lại lần thứ attempt. */
    default void onReconnecting(int attempt) {
    }

    default void onError(TransportFailure failure) {
    }
}

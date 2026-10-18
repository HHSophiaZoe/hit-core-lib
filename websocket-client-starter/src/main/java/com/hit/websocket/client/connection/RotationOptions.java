package com.hit.websocket.client.connection;

import java.time.Duration;
import java.util.Objects;

/**
 * Cấu hình xoay session của RotatingWebSocketClient.
 *
 * @param interval      thời gian dùng một session trước khi thay bằng session mới (đặt nhỏ hơn giới hạn của provider)
 * @param readyTimeout  thời gian tối đa chờ session mới sẵn sàng; quá hạn thì giữ session cũ và thử lại sau retryDelay
 * @param drainDelay    thời gian session cũ còn nhận message sau khi session mới đã thay, để không mất message đang trên đường
 * @param dedupeWindow  thời gian nhớ message để lọc trùng sau khi session cũ đã đóng
 * @param retryDelay    thời gian chờ trước khi thử xoay lại khi session mới không sẵn sàng
 */
public record RotationOptions(Duration interval, Duration readyTimeout, Duration drainDelay, Duration dedupeWindow,
                              Duration retryDelay) {

    public RotationOptions {
        positive(interval, "interval");
        positive(readyTimeout, "readyTimeout");
        positive(drainDelay, "drainDelay");
        positive(dedupeWindow, "dedupeWindow");
        positive(retryDelay, "retryDelay");
    }

    public static RotationOptions every(Duration interval) {
        return new RotationOptions(interval, Duration.ofSeconds(30), Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofMinutes(1));
    }

    private static void positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || value.isZero()) throw new IllegalArgumentException(name + " must be positive");
    }
}

package com.hit.chatbot.telegram.api;

/**
 * Vỏ chung của mọi response Bot API.
 */
public record TelegramResponse<T>(boolean ok, T result, Integer errorCode, String description, Parameters parameters) {

    /**
     * Thông tin thêm khi bị từ chối; {@code retryAfter} (giây) có khi bị giới hạn tốc độ (429).
     */
    public record Parameters(Integer retryAfter) {
    }
}

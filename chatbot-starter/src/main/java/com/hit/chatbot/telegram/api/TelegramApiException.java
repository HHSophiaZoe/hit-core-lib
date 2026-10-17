package com.hit.chatbot.telegram.api;

import java.time.Duration;

public class TelegramApiException extends RuntimeException {

    /**
     * How long Telegram asks to wait before the next call, when it refused one for rate limiting (429).
     */
    private final Duration retryAfter;

    public TelegramApiException(String message) {
        this(message, (Duration) null);
    }

    public TelegramApiException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public TelegramApiException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfter = null;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}

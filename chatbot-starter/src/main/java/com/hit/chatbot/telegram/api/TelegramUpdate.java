package com.hit.chatbot.telegram.api;

public record TelegramUpdate(long updateId, TelegramMessage message) {

    /**
     * Whether the update carries a message; other update kinds (edited message, callback query...) have none.
     */
    public boolean hasMessage() {
        return message != null;
    }
}

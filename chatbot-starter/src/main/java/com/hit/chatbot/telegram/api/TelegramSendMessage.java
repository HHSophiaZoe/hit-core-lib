package com.hit.chatbot.telegram.api;

import lombok.Builder;

/**
 * Body của Bot API {@code sendMessage}; field null không được gửi đi.
 */
@Builder
public record TelegramSendMessage(
        String chatId,
        Integer messageThreadId,
        String text,
        String parseMode
) {
}

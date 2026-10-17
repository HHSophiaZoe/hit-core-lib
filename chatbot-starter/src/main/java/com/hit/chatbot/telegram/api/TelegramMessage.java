package com.hit.chatbot.telegram.api;

public record TelegramMessage(
        long messageId,
        Integer messageThreadId,
        Boolean isTopicMessage,
        TelegramChat chat,
        String text
) {
}

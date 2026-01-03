package com.hit.chatbot.data.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    private String chatId;
    private String title;
    private String content;
    @Builder.Default
    private MessageType type = MessageType.INFO;

    public enum MessageType {
        INFO, WARN, ERROR
    }
}

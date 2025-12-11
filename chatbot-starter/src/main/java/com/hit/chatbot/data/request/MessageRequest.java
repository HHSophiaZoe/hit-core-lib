package com.hit.chatbot.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    private String channelId;
    private String title;
    private String content;
    private MessageType type;

    public enum MessageType {
        INFO, WARN, ERROR
    }
}

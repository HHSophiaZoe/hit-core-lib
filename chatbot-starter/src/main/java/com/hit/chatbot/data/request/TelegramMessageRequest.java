package com.hit.chatbot.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TelegramMessageRequest extends MessageRequest {
    
    private String parseMode; // HTML, Markdown, MarkdownV2
    private boolean disableNotification;
    private boolean protectContent;

}
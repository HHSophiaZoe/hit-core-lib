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
    
    private ParseMode parseMode;
    private boolean disableNotification;
    private boolean protectContent;

    public enum ParseMode {
        HTML, MARKDOWN, MARKDOWN_V2
    }

}
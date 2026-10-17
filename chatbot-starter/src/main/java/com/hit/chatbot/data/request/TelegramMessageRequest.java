package com.hit.chatbot.data.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A Telegram message in HTML: the title is plain text, the content is HTML with the tags Telegram supports; dynamic
 * text needs no escaping, it is sanitized before sending (see {@code TelegramHtmlUtils}).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TelegramMessageRequest extends MessageRequest {

    private boolean disableNotification;
    private boolean protectContent;

}

package com.hit.chatbot.data.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String chatId;
    private String command;
    private String content;

}

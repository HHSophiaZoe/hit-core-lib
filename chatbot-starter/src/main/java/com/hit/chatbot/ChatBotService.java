package com.hit.chatbot;

import com.hit.chatbot.data.request.MessageRequest;

public interface ChatBotService {

    void sendMessage(String channelId, String content);

    void sendMessage(MessageRequest request);

    void sendPrivateMessage(String userId, String content);

}

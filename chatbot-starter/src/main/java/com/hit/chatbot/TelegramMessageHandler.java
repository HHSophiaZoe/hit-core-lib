//package com.hit.chatbot;
//
//import com.hit.chatbot.annotation.ChatBotMessageListener;
//import com.hit.chatbot.data.response.MessageResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class TelegramMessageHandler {
//
//    @ChatBotMessageListener(
//            platforms = ChatBotMessageListener.Platform.TELEGRAM,
//            command = "/start"
//    )
//    public void handleStartCommand(MessageResponse message) {
//        log.info("User {} started the bot", message.getChatId());
//    }
//
//    @ChatBotMessageListener(
//            platforms = ChatBotMessageListener.Platform.TELEGRAM,
//            command = "/help"
//    )
//    public void handleHelpCommand(MessageResponse message) {
//        log.info("User {} requested help", message.getChatId());
//    }
//
//    @ChatBotMessageListener(
//            platforms = ChatBotMessageListener.Platform.TELEGRAM,
//            ids = {"123456789", "987654321"}
//    )
//    public void handleSpecificChatMessage(MessageResponse message) {
//        log.info("Message from specific chat: {}", message.getContent());
//    }
//
//    @ChatBotMessageListener(platforms = ChatBotMessageListener.Platform.TELEGRAM)
//    public void handleAllTelegramMessages(MessageResponse message) {
//        log.info("Received Telegram message: {}", message.getContent());
//    }
//}
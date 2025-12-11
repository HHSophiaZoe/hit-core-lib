//package com.hit.chatbot;
//
//import com.hit.chatbot.annotation.ChatBotMessageListener;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.Message;
//
//@Slf4j
//@Component
//public class TelegramMessageHandler {
//
//    @ChatBotMessageListener(
//        platforms = ChatBotMessageListener.Platform.TELEGRAM,
//        command = "/start"
//    )
//    public void handleStartCommand(Message message) {
//        log.info("User {} started the bot", message.getFrom().getId());
//    }
//
//    @ChatBotMessageListener(
//        platforms = ChatBotMessageListener.Platform.TELEGRAM,
//        command = "/help"
//    )
//    public void handleHelpCommand(Message message) {
//        log.info("User {} requested help", message.getFrom().getId());
//    }
//
//    @ChatBotMessageListener(
//        platforms = ChatBotMessageListener.Platform.TELEGRAM,
//        ids = {"123456789", "987654321"}
//    )
//    public void handleSpecificChatMessage(Message message) {
//        log.info("Message from specific chat: {}", message.getText());
//    }
//
//    @ChatBotMessageListener(platforms = ChatBotMessageListener.Platform.TELEGRAM)
//    public void handleAllTelegramMessages(Message message) {
//        log.info("Received Telegram message: {}", message.getText());
//    }
//}
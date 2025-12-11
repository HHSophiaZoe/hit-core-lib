//package com.hit.chatbot;
//
//import com.hit.chatbot.annotation.ChatBotMessageListener;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class DiscordMessageHandler {
//
//    @ChatBotMessageListener(
//        platforms = ChatBotMessageListener.Platform.DISCORD,
//        command = "!ping"
//    )
//    public void handlePingCommand(Object message) {
//        log.info("Received ping command on Discord");
//    }
//
//    @ChatBotMessageListener(
//        platforms = ChatBotMessageListener.Platform.DISCORD,
//        ids = {"channel-id-1", "channel-id-2"}
//    )
//    public void handleDiscordChannelMessage(Object message) {
//        log.info("Received message from specific Discord channel");
//    }
//
//    @ChatBotMessageListener(
//        platforms = {
//            ChatBotMessageListener.Platform.TELEGRAM,
//            ChatBotMessageListener.Platform.DISCORD
//        }
//    )
//    public void handleBothPlatforms(Object message) {
//        log.info("Received message from both Telegram and Discord");
//    }
//}
package com.hit.chatbot.telegram.config;

import com.hit.chatbot.annotation.ConditionalOnTelegramEnable;
import com.hit.chatbot.telegram.TelegramChatBotServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnTelegramEnable
public class BotInitializer {

    private final TelegramChatBotServiceImpl telegramBot;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBot);
            log.info("✓✓✓ Telegram bot registered successfully! ✓✓✓");
        } catch (TelegramApiException e) {
            log.error("✗✗✗ Error registering bot: {}", e.getMessage(), e);
        }
    }

}

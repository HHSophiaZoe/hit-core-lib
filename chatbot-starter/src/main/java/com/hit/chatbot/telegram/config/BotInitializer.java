package com.hit.chatbot.telegram.config;

import com.hit.chatbot.annotation.ConditionalOnTelegramEnable;
import com.hit.chatbot.telegram.TelegramChatBotServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnTelegramEnable
public class BotInitializer implements CommandLineRunner {

    private final TelegramBotsApi telegramBotsApi;
    private final TelegramChatBotServiceImpl telegramBot;

    @Override
    public void run(String... args) {
        try {
            telegramBotsApi.registerBot(telegramBot);
            log.info("✓✓✓ Telegram bot registered successfully! ✓✓✓");
        } catch (TelegramApiException e) {
            log.error("✗✗✗ Error registering bot: {}", e.getMessage(), e);
        }
    }
}

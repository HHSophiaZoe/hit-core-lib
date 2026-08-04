package com.hit.chatbot.telegram.config;

import com.hit.chatbot.annotation.ConditionalOnTelegramEnable;
import com.hit.chatbot.telegram.properties.TelegramProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@ConditionalOnTelegramEnable
public class TelegramClientConfig {

    @Bean
    public TelegramClient telegramClient(TelegramProperties properties) {
        return new OkHttpTelegramClient(properties.getToken());
    }
}

package com.hit.chatbot.telegram.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "bot.telegram")
public class TelegramProperties {

    private boolean enable;
    private String token;
    private String username;

}
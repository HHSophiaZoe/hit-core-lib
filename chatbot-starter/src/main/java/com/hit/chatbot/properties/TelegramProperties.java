package com.hit.chatbot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private boolean enable;
    private String botToken;
    private String botUsername;

}
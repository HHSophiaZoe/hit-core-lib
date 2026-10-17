package com.hit.chatbot.telegram.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "chatbot.telegram")
public class TelegramProperties {

    private boolean enable;
    private String token;
    private String username;
    private String baseUrl = "https://api.telegram.org";
    private Duration pollTimeout = Duration.ofSeconds(50);
    private Duration errorBackoff = Duration.ofSeconds(5);
    /**
     * Longest wait before sending again a message Telegram refused for rate limiting (429); when Telegram asks for a
     * longer wait the message is dropped and logged.
     */
    private Duration maxRetryWait = Duration.ofSeconds(30);

}

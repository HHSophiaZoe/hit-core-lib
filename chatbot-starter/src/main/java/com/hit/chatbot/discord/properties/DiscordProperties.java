package com.hit.chatbot.discord.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "chatbot.discord")
public class DiscordProperties {
    private boolean enable;
    private String token;
    /**
     * How long a send waits for the bot to finish connecting; the bot connects in the background, so the application
     * starts even when Discord is unreachable.
     */
    private Duration readyTimeout = Duration.ofSeconds(30);
}
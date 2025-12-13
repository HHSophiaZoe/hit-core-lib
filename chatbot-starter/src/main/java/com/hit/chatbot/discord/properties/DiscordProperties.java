package com.hit.chatbot.discord.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "bot.discord")
public class DiscordProperties {
    private Boolean enable = true;
    private String token;
    private Map<String, String> channels = new HashMap<>();
}
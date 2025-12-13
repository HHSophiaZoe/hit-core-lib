package com.hit.chatbot.discord.config;

import com.hit.chatbot.annotation.ConditionalOnDiscordEnable;
import com.hit.chatbot.dispatcher.ChatBotMessageDispatcher;
import com.hit.chatbot.discord.properties.DiscordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnDiscordEnable
public class DiscordConfig {

    private final DiscordProperties discordProperties;

    private final ChatBotMessageDispatcher dispatcher;

    @Bean
    public JDA discord() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(discordProperties.getToken())
                .setActivity(Activity.playing("Trading Application"))
                .addEventListeners((EventListener) event -> {
                    if (event instanceof ReadyEvent) {
                        this.onReady((ReadyEvent) event);
                    } else if (event instanceof MessageReceivedEvent) {
                        this.onMessageReceived((MessageReceivedEvent) event);
                    }
                })
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                .build();
        jda.awaitReady();
        return jda;
    }

    private void onReady(ReadyEvent event) {
        log.info("Bot {} ready!", event.getJDA().getSelfUser().getName());
    }

    private void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        TextChannel channel = event.getChannel().asTextChannel();

        if (message.startsWith("!hello")) {
            channel.sendMessage("Xin chào " + event.getAuthor().getAsMention() + "! 👋").queue();
        }

        if (message.startsWith("!ping")) {
            long ping = event.getJDA().getGatewayPing();
            channel.sendMessage("Pong! Ping: " + ping + "ms").queue();
        }

        dispatcher.dispatchDiscordMessage(event);
    }

}

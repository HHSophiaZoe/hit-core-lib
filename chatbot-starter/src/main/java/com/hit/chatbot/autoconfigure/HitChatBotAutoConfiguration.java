package com.hit.chatbot.autoconfigure;

import com.hit.chatbot.ChatBotAnnotationScanner;
import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.ChatBotMessageListenerResolver;
import com.hit.chatbot.annotation.ConditionalOnDiscordEnable;
import com.hit.chatbot.annotation.ConditionalOnTelegramEnable;
import com.hit.chatbot.discord.DiscordChatBotServiceImpl;
import com.hit.chatbot.discord.properties.DiscordProperties;
import com.hit.chatbot.telegram.TelegramChatBotServiceImpl;
import com.hit.chatbot.telegram.TelegramUpdatePoller;
import com.hit.chatbot.telegram.api.TelegramApiClient;
import com.hit.chatbot.telegram.properties.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;

/**
 * Chat bots: Telegram ({@code chatbot.telegram.*}) and Discord ({@code chatbot.discord.*}), plus dispatching of
 * incoming messages to {@code @ChatBotMessageListener} methods.
 */
@AutoConfiguration
@EnableConfigurationProperties({TelegramProperties.class, DiscordProperties.class})
public class HitChatBotAutoConfiguration {

    @Bean
    public static ChatBotAnnotationScanner chatBotAnnotationScanner(ObjectProvider<ChatBotMessageDispatcher> dispatcher) {
        return new ChatBotAnnotationScanner(dispatcher);
    }

    @Bean
    public ChatBotMessageListenerResolver chatBotMessageListenerResolver(ConfigurableBeanFactory beanFactory) {
        return new ChatBotMessageListenerResolver(beanFactory);
    }

    @Bean
    public ChatBotMessageDispatcher chatBotMessageDispatcher(ChatBotMessageListenerResolver resolver, TaskExecutor taskExecutor) {
        return new ChatBotMessageDispatcher(resolver, taskExecutor);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnTelegramEnable
    static class TelegramConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TelegramApiClient telegramApiClient(TelegramProperties properties) {
            return new TelegramApiClient(properties.getBaseUrl(), properties.getToken(), properties.getMaxRetryWait());
        }

        @Bean
        public TelegramUpdatePoller telegramUpdatePoller(TelegramApiClient client, ChatBotMessageDispatcher dispatcher,
                                                         TelegramProperties properties) {
            return new TelegramUpdatePoller(client, dispatcher, properties.getPollTimeout(), properties.getErrorBackoff());
        }

        // Telegram is the default when both bots are enabled
        @Primary
        @Bean
        public TelegramChatBotServiceImpl telegramChatBotService(TelegramApiClient telegramApiClient, TaskExecutor taskExecutor) {
            return new TelegramChatBotServiceImpl(telegramApiClient, taskExecutor);
        }
    }

    @Slf4j
    @ConditionalOnDiscordEnable
    @Configuration(proxyBeanMethods = false)
    static class DiscordConfiguration {

        @Bean(destroyMethod = "shutdown")
        /**
         * Connects in the background: the application does not wait for Discord, sends wait up to ready-timeout.
         */
        public JDA discord(DiscordProperties properties, ChatBotMessageDispatcher dispatcher) {
            return JDABuilder.createDefault(properties.getToken())
                    .setActivity(Activity.playing("Discord Chatbot"))
                    .addEventListeners((EventListener) event -> {
                        if (event instanceof ReadyEvent readyEvent) {
                            log.info("Bot {} ready!", readyEvent.getJDA().getSelfUser().getName());
                        } else if (event instanceof MessageReceivedEvent messageEvent) {
                            onMessageReceived(messageEvent, dispatcher);
                        }
                    })
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .build();
        }

        @Bean
        public DiscordChatBotServiceImpl discordChatBotService(JDA jda, TaskExecutor taskExecutor, DiscordProperties properties) {
            return new DiscordChatBotServiceImpl(jda, taskExecutor, properties.getReadyTimeout());
        }

        private static void onMessageReceived(MessageReceivedEvent event, ChatBotMessageDispatcher dispatcher) {
            if (event.getAuthor().isBot()) {
                return;
            }
            dispatcher.dispatchDiscordMessage(event);
        }
    }
}

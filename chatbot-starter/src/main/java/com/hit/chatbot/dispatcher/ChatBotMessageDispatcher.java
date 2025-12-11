package com.hit.chatbot.dispatcher;

import com.hit.chatbot.annotation.ChatBotMessageListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBotMessageDispatcher {

    private final ApplicationContext applicationContext;
    private final Map<String, List<ListenerMethod>> listenerMap = new HashMap<>();
    private boolean initialized = false;

    public void dispatchTelegramUpdate(Update update) {
        if (!initialized) {
            initializeListeners();
        }

        if (update.hasMessage()) {
            Message message = update.getMessage();
            handleTelegramMessage(message);
        }
    }

    public void dispatchDiscordMessage(MessageReceivedEvent event) {
        if (!initialized) {
            initializeListeners();
        }
        handleDiscordMessage(event);
    }

    private void handleTelegramMessage(Message message) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText() != null ? message.getText() : "";

        log.info("Handling Telegram message from chat: {}, text: {}", chatId, text);

        for (List<ListenerMethod> listeners : listenerMap.values()) {
            for (ListenerMethod listener : listeners) {
                if (listener.isPlatformSupported(ChatBotMessageListener.Platform.TELEGRAM) && matchesTelegram(chatId, text, listener)) {
                    invokeTelegramListener(listener, message);
                }
            }
        }
    }

    private void handleDiscordMessage(MessageReceivedEvent event) {
        String channelId = event.getChannel().getId();
        String text = event.getMessage().getContentRaw();

        log.info("Handling Discord message from channel: {}, text: {}", channelId, text);
        for (List<ListenerMethod> listeners : listenerMap.values()) {
            for (ListenerMethod listener : listeners) {
                if (listener.isPlatformSupported(ChatBotMessageListener.Platform.DISCORD) && matchesDiscord(channelId, text, listener)) {
                    invokeDiscordListener(listener, event);
                }
            }
        }
    }

    private boolean matchesTelegram(String chatId, String text, ListenerMethod listener) {
        // Check chatId
        if (listener.annotation.ids().length > 0) {
            boolean chatIdMatches = Arrays.asList(listener.annotation.ids()).contains(chatId);
            if (!chatIdMatches) return false;
        }

        // Check command
        if (!listener.annotation.command().isEmpty()) {
            return text.startsWith(listener.annotation.command());
        }

        return true;
    }

    private boolean matchesDiscord(String channelId, String text, ListenerMethod listener) {
        // Check channelId
        if (listener.annotation.ids().length > 0) {
            boolean channelIdMatches = Arrays.asList(listener.annotation.ids()).contains(channelId);
            if (!channelIdMatches) return false;
        }

        // Check command
        if (!listener.annotation.command().isEmpty()) {
            return text.startsWith(listener.annotation.command());
        }

        return true;
    }

    private void invokeTelegramListener(ListenerMethod listener, Message message) {
        try {
            log.info("Invoking Telegram listener: {}", listener.method.getName());
            listener.method.invoke(listener.bean, message);
        } catch (Exception e) {
            log.error("Error invoking Telegram listener: {}", listener.method.getName(), e);
        }
    }

    private void invokeDiscordListener(ListenerMethod listener, Object message) {
        try {
            log.info("Invoking Discord listener: {}", listener.method.getName());
            listener.method.invoke(listener.bean, message);
        } catch (Exception e) {
            log.error("Error invoking Discord listener: {}", listener.method.getName(), e);
        }
    }

    private void initializeListeners() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = bean.getClass().getDeclaredMethods();

            for (Method method : methods) {
                ChatBotMessageListener annotation = method.getAnnotation(ChatBotMessageListener.class);
                if (annotation != null) {
                    method.setAccessible(true);
                    ListenerMethod listenerMethod = new ListenerMethod(bean, method, annotation);
                    String key = beanName + ":" + method.getName();
                    listenerMap.computeIfAbsent(key, k -> new ArrayList<>()).add(listenerMethod);
                    log.info("Registered listener: {} for platforms: {} with command: {}",
                            method.getName(), Arrays.toString(annotation.platforms()), annotation.command());
                }
            }
        }

        initialized = true;
    }

    private static class ListenerMethod {
        Object bean;
        Method method;
        ChatBotMessageListener annotation;

        ListenerMethod(Object bean, Method method, ChatBotMessageListener annotation) {
            this.bean = bean;
            this.method = method;
            this.annotation = annotation;
        }

        boolean isPlatformSupported(ChatBotMessageListener.Platform platform) {
            return Arrays.asList(annotation.platforms()).contains(platform);
        }
    }
}
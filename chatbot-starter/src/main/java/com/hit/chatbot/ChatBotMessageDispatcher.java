package com.hit.chatbot;

import com.hit.chatbot.annotation.ChatBotMessageListener;
import com.hit.chatbot.annotation.ChatBotMessageListener.Platform;
import com.hit.chatbot.data.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBotMessageDispatcher {

    private final AsyncTaskExecutor taskExecutor;

    private final List<ListenerMethod> listeners = new ArrayList<>();

    public void addListener(String beanName, Object bean) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        Method[] methods = targetClass.getDeclaredMethods();
        for (Method method : methods) {
            ChatBotMessageListener annotation = method.getAnnotation(ChatBotMessageListener.class);
            if (annotation != null) {
                method.setAccessible(true);
                ChatBotMessageDispatcher.ListenerMethod listenerMethod = new ChatBotMessageDispatcher.ListenerMethod(bean, method, annotation);
                listeners.add(listenerMethod);
                log.debug("Registered listener: {} from bean {} for platforms: {} with command: {}",
                        method.getName(), beanName, Arrays.toString(annotation.platforms()), annotation.commands());
            }
        }
    }

    public void dispatchTelegramUpdate(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            handleTelegramMessage(message);
        }
    }

    public void dispatchDiscordMessage(MessageReceivedEvent event) {
        handleDiscordMessage(event);
    }

    private void handleTelegramMessage(Message message) {
        String chatId;
        if (Boolean.TRUE.equals(message.getIsTopicMessage())) {
            chatId = message.getChatId() + "_" + message.getMessageThreadId();
        } else {
            chatId = String.valueOf(message.getChatId());
        }
        String text = message.getText() != null ? message.getText() : StringUtils.EMPTY;

        log.trace("Handling Telegram message from message: {}", message);
        for (ListenerMethod listener : listeners) {
            if (listener.isPlatformSupported(Platform.TELEGRAM) && listener.isMatchesMessage(chatId, text)) {
                Pair<String, String> commandContent = this.getCommandContent(text);
                MessageResponse messageResponse = MessageResponse.builder()
                        .chatId(chatId)
                        .command(commandContent.getKey())
                        .content(commandContent.getValue())
                        .build();
                this.invokeListener(listener, messageResponse);
            }
        }
    }

    private void handleDiscordMessage(MessageReceivedEvent event) {
        String channelId = event.getChannel().getId();
        String text = event.getMessage().getContentRaw();

        log.trace("Handling Discord message from event: {}", event);
        for (ListenerMethod listener : listeners) {
            if (listener.isPlatformSupported(Platform.DISCORD) && listener.isMatchesMessage(channelId, text)) {
                Pair<String, String> commandContent = this.getCommandContent(text);
                MessageResponse messageResponse = MessageResponse.builder()
                        .chatId(channelId)
                        .command(commandContent.getKey())
                        .content(commandContent.getValue())
                        .build();
                this.invokeListener(listener, messageResponse);
            }
        }
    }

    private Pair<String, String> getCommandContent(final String text) {
        if (StringUtils.isBlank(text) || !text.startsWith("/")) {
            return Pair.of(StringUtils.EMPTY, text);
        }

        String trimmed = text.trim();
        int firstSpace = trimmed.indexOf(StringUtils.SPACE);

        if (firstSpace == -1) { // only command
            return Pair.of(trimmed, StringUtils.EMPTY);
        }

        String command = trimmed.substring(0, firstSpace);
        String content = trimmed.substring(firstSpace + 1).trim();

        return Pair.of(command, content);
    }

    private void invokeListener(ListenerMethod listener, MessageResponse message) {
        taskExecutor.execute(() -> {
            try {
                log.trace("Invoking listener {}: {}", listener.method.getClass(), listener.method.getName());
                listener.method.invoke(listener.bean, message);
            } catch (Exception e) {
                log.trace("Error invoking listener: {}", listener.method.getName(), e);
            }
        });
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

        boolean isPlatformSupported(Platform platform) {
            return Arrays.asList(this.annotation.platforms()).contains(platform);
        }

        private boolean isMatchesMessage(String id, String text) {
            // Check chatId
            if (this.annotation.ids().length > 0) {
                boolean chatIdMatches = Arrays.asList(this.annotation.ids()).contains(id);
                if (!chatIdMatches) return false;
            }

            // Check command
            if (this.annotation.commands().length > 0) {
                return Arrays.stream(this.annotation.commands()).anyMatch(text::startsWith);
            }

            return true;
        }
    }
}
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBotMessageDispatcher {

    private final ChatBotMessageListenerResolver resolver;

    private final TaskExecutor taskExecutor;

    private final List<ListenerMethod> listeners = new ArrayList<>();

    public void addListener(String beanName, Object bean) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        Method[] methods = targetClass.getDeclaredMethods();
        for (Method method : methods) {
            ChatBotMessageListener annotation = method.getAnnotation(ChatBotMessageListener.class);
            if (annotation != null) {
                method.setAccessible(true);
                ListenerMethod listenerMethod =
                        new ListenerMethod(bean, method, annotation, resolver);
                listeners.add(listenerMethod);
                log.debug("Registered listener: {} from bean {} for platforms: {} with command: {}",
                        method.getName(), beanName, Arrays.toString(annotation.platforms()), annotation.commands());
            }
        }
    }

    public void dispatchTelegramUpdate(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            this.handleTelegramMessage(message);
        }
    }

    public void dispatchDiscordMessage(MessageReceivedEvent event) {
        this.handleDiscordMessage(event);
    }

    private void handleTelegramMessage(Message message) {
        String chatId;
        if (Boolean.TRUE.equals(message.getIsTopicMessage())) {
            chatId = message.getChatId() + "_" + message.getMessageThreadId();
        } else {
            chatId = String.valueOf(message.getChatId());
        }
        String text = message.getText() != null ? message.getText() : StringUtils.EMPTY;
        Pair<String, String> commandContent = ChatBotCommandParser.parse(text);

        log.trace("Handling Telegram message from message: {}", message);
        for (ListenerMethod listener : listeners) {
            if (listener.isPlatformSupported(Platform.TELEGRAM)
                    && listener.isMatchesMessage(chatId, commandContent.getLeft())) {
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
        Pair<String, String> commandContent = ChatBotCommandParser.parse(text);

        log.trace("Handling Discord message from event: {}", event);
        for (ListenerMethod listener : listeners) {
            if (listener.isPlatformSupported(Platform.DISCORD)
                    && listener.isMatchesMessage(channelId, commandContent.getLeft())) {
                MessageResponse messageResponse = MessageResponse.builder()
                        .chatId(channelId)
                        .command(commandContent.getKey())
                        .content(commandContent.getValue())
                        .build();
                this.invokeListener(listener, messageResponse);
            }
        }
    }

    private void invokeListener(ListenerMethod listener, MessageResponse message) {
        taskExecutor.execute(() -> {
            try {
                log.trace("Invoking listener {}: {}", listener.method.getClass(), listener.method.getName());
                listener.method.invoke(listener.bean, message);
            } catch (Exception e) {
                log.error("Error invoking listener: {}", listener.method.getName(), e);
            }
        });
    }

    private static class ListenerMethod {
        Object bean;
        Method method;
        ChatBotMessageListener annotation;
        String[] resolvedIds;        // Giá trị ids đã được resolve
        String[] resolvedCommands;   // Giá trị commands đã được resolve

        ListenerMethod(Object bean, Method method, ChatBotMessageListener annotation, ChatBotMessageListenerResolver resolver) {
            this.bean = bean;
            this.method = method;
            this.annotation = annotation;
            // Resolve ids và commands từ annotation
            this.resolvedIds = resolver.resolveValues(annotation.ids());
            this.resolvedCommands = resolver.resolveValues(annotation.commands());
        }

        boolean isPlatformSupported(Platform platform) {
            return Arrays.asList(this.annotation.platforms()).contains(platform);
        }

        private boolean isMatchesMessage(String id, String command) {
            // Check chatId
            if (this.resolvedIds != null && this.resolvedIds.length > 0) {
                boolean chatIdMatches = Arrays.asList(this.resolvedIds).contains(id);
                if (!chatIdMatches) {
                    return false;
                }
            }

            // Check command
            if (this.resolvedCommands != null && this.resolvedCommands.length > 0) {
                return Arrays.stream(this.resolvedCommands)
                        .anyMatch(expectedCommand -> ChatBotCommandParser.matches(command, expectedCommand));
            }

            return true;
        }
    }
}

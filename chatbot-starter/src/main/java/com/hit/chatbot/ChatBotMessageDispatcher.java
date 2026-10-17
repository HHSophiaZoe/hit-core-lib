package com.hit.chatbot;

import com.hit.chatbot.annotation.ChatBotMessageListener;
import com.hit.chatbot.annotation.ChatBotMessageListener.Platform;
import com.hit.chatbot.data.response.CommandHelp;
import com.hit.chatbot.data.response.MessageResponse;
import com.hit.chatbot.telegram.api.TelegramMessage;
import com.hit.chatbot.telegram.api.TelegramUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
public class ChatBotMessageDispatcher {

    private final ChatBotMessageListenerResolver resolver;

    private final TaskExecutor taskExecutor;

    /**
     * Listeners are added while beans are created and read by the bot threads, which may already receive messages
     * (Discord connects as soon as its bean exists).
     */
    private final List<ListenerMethod> listeners = new CopyOnWriteArrayList<>();

    /**
     * The {@link ChatBotMessageListener} methods of a class, its superclasses and interfaces included.
     */
    static Map<Method, ChatBotMessageListener> findListenerMethods(Class<?> targetClass) {
        return MethodIntrospector.selectMethods(targetClass, (MethodIntrospector.MetadataLookup<ChatBotMessageListener>)
                method -> AnnotatedElementUtils.findMergedAnnotation(method, ChatBotMessageListener.class));
    }

    /**
     * Registers the listener methods of a bean. The bean may be an AOP proxy: its methods are called through the
     * proxy, so the advice ({@code @Transactional}...) applies.
     *
     * @throws IllegalStateException when the bean is an interface-based proxy and a listener method is not on the
     *                               interface
     */
    public void addListener(String beanName, Object bean) {
        findListenerMethods(AopProxyUtils.ultimateTargetClass(bean)).forEach((method, annotation) -> {
            Method invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
            ReflectionUtils.makeAccessible(invocable);
            listeners.add(new ListenerMethod(bean, invocable, annotation, resolver));
            log.debug("Registered listener: {} from bean {} for platforms: {} with command: {}",
                    method.getName(), beanName, Arrays.toString(annotation.platforms()), annotation.commands());
        });
    }

    /**
     * Các command đang lắng nghe ở channel, dùng để dựng /help.
     */
    public List<CommandHelp> getCommandHelps(Platform platform, String chatId) {
        return listeners.stream()
                .filter(listener -> listener.isPlatformSupported(platform) && listener.isListeningChat(chatId))
                .flatMap(listener -> Arrays.stream(listener.resolvedCommands)
                        .map(command -> new CommandHelp(command, listener.annotation.description())))
                .toList();
    }

    public void dispatchTelegramUpdate(TelegramUpdate update) {
        if (update.hasMessage()) {
            this.handleTelegramMessage(update.message());
        }
    }

    public void dispatchDiscordMessage(MessageReceivedEvent event) {
        this.handleDiscordMessage(event);
    }

    private void handleTelegramMessage(TelegramMessage message) {
        String chatId;
        if (Boolean.TRUE.equals(message.isTopicMessage())) {
            chatId = message.chat().id() + "_" + message.messageThreadId();
        } else {
            chatId = String.valueOf(message.chat().id());
        }
        String text = message.text() != null ? message.text() : StringUtils.EMPTY;
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

        private boolean isListeningChat(String id) {
            return this.resolvedIds == null || this.resolvedIds.length == 0 || Arrays.asList(this.resolvedIds).contains(id);
        }

        private boolean isMatchesMessage(String id, String command) {
            if (!this.isListeningChat(id)) {
                return false;
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

package com.hit.chatbot;

import com.hit.chatbot.annotation.ChatBotMessageListener;
import com.hit.chatbot.annotation.ChatBotMessageListener.Platform;
import com.hit.chatbot.data.response.MessageResponse;
import com.hit.chatbot.telegram.api.TelegramChat;
import com.hit.chatbot.telegram.api.TelegramMessage;
import com.hit.chatbot.telegram.api.TelegramUpdate;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatBotMessageDispatcherTest {

    // Listeners run on the calling thread, so each test sees their effect at once
    private final ChatBotMessageDispatcher dispatcher = new ChatBotMessageDispatcher(
            new ChatBotMessageListenerResolver(new DefaultListableBeanFactory()), Runnable::run);

    private final List<String> calls = new CopyOnWriteArrayList<>();

    @Test
    void listenerDeclaredInASuperclassIsRegistered() {
        dispatcher.addListener("child", new ChildListener(calls));

        dispatcher.dispatchTelegramUpdate(telegram("/status"));

        assertThat(calls).containsExactly("status");
    }

    @Test
    void listenerDeclaredOnAnInterfaceIsRegistered() {
        dispatcher.addListener("impl", new CommandsImpl(calls));

        dispatcher.dispatchTelegramUpdate(telegram("/ping"));

        assertThat(calls).containsExactly("ping");
    }

    @Test
    void classProxyIsCalledThroughItsAdvice() {
        AtomicInteger advised = new AtomicInteger();
        ProxyFactory factory = new ProxyFactory(new ChildListener(calls));
        factory.setProxyTargetClass(true);
        factory.addAdvice((MethodInterceptor) invocation -> {
            advised.incrementAndGet();
            return invocation.proceed();
        });
        Object proxy = factory.getProxy();
        assertThat(AopUtils.isCglibProxy(proxy)).isTrue();

        dispatcher.addListener("proxied", proxy);
        dispatcher.dispatchTelegramUpdate(telegram("/status"));

        assertThat(calls).containsExactly("status");
        assertThat(advised).hasValue(1);
    }

    @Test
    void interfaceProxyIsCalledThroughTheInterface() {
        ProxyFactory factory = new ProxyFactory(new CommandsImpl(calls));
        factory.setInterfaces(Commands.class);
        Object proxy = factory.getProxy();
        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();

        dispatcher.addListener("proxied", proxy);
        dispatcher.dispatchTelegramUpdate(telegram("/ping"));

        assertThat(calls).containsExactly("ping");
    }

    @Test
    void interfaceProxyHidingAListenerFailsWhenRegistered() {
        ProxyFactory factory = new ProxyFactory(new HiddenListener());
        factory.setInterfaces(Runnable.class);

        assertThatThrownBy(() -> dispatcher.addListener("hidden", factory.getProxy()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listenerAddedWhileDispatchingDoesNotBreakTheDispatch() {
        dispatcher.addListener("adding", new AddingListener(dispatcher, calls));

        dispatcher.dispatchTelegramUpdate(telegram("/status"));
        dispatcher.dispatchTelegramUpdate(telegram("/status"));

        // The listener added during the first dispatch answers from the second one on
        assertThat(calls).containsExactly("adding", "adding", "status");
    }

    @Test
    void updateWithoutMessageIsIgnored() {
        dispatcher.addListener("child", new ChildListener(calls));

        dispatcher.dispatchTelegramUpdate(new TelegramUpdate(1, null));

        assertThat(calls).isEmpty();
    }

    private static TelegramUpdate telegram(String text) {
        return new TelegramUpdate(1, new TelegramMessage(1, null, false, new TelegramChat(-100), text));
    }

    static class BaseListener {

        final List<String> calls;

        BaseListener(List<String> calls) {
            this.calls = calls;
        }

        @ChatBotMessageListener(platforms = Platform.TELEGRAM, commands = "/status")
        public void status(MessageResponse message) {
            calls.add("status");
        }
    }

    static class ChildListener extends BaseListener {

        ChildListener(List<String> calls) {
            super(calls);
        }
    }

    interface Commands {

        @ChatBotMessageListener(platforms = Platform.TELEGRAM, commands = "/ping")
        void ping(MessageResponse message);
    }

    static class CommandsImpl implements Commands {

        private final List<String> calls;

        CommandsImpl(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public void ping(MessageResponse message) {
            calls.add("ping");
        }
    }

    static class HiddenListener implements Runnable {

        @Override
        public void run() {
        }

        @ChatBotMessageListener(platforms = Platform.TELEGRAM, commands = "/hidden")
        public void hidden(MessageResponse message) {
        }
    }

    static class AddingListener {

        private final ChatBotMessageDispatcher dispatcher;

        private final List<String> calls;

        private boolean added;

        AddingListener(ChatBotMessageDispatcher dispatcher, List<String> calls) {
            this.dispatcher = dispatcher;
            this.calls = calls;
        }

        @ChatBotMessageListener(platforms = Platform.TELEGRAM, commands = "/status")
        public void status(MessageResponse message) {
            calls.add("adding");
            if (!added) {
                added = true;
                dispatcher.addListener("late", new ChildListener(calls));
            }
        }
    }
}

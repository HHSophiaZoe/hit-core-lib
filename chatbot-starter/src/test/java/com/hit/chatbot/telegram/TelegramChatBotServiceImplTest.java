package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.telegram.properties.TelegramProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramChatBotServiceImplTest {

    private RecordingDispatcher dispatcher;
    private TelegramClient telegramClient;
    private AtomicReference<SendMessage> sentMessage;
    private TelegramChatBotServiceImpl service;

    @BeforeEach
    void setUp() {
        TelegramProperties properties = new TelegramProperties();
        properties.setToken("bot-token");
        dispatcher = new RecordingDispatcher();
        sentMessage = new AtomicReference<>();
        telegramClient = (TelegramClient) Proxy.newProxyInstance(
                TelegramClient.class.getClassLoader(),
                new Class<?>[]{TelegramClient.class},
                (proxy, method, args) -> {
                    if (args != null && args.length == 1 && args[0] instanceof SendMessage message) {
                        sentMessage.set(message);
                    }
                    return null;
                });
        service = new TelegramChatBotServiceImpl(properties, dispatcher, telegramClient);
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    @Test
    void implementsChatBotAndLongPollingContracts() {
        assertThat(service)
                .isInstanceOf(ChatBotService.class)
                .isInstanceOf(SpringLongPollingBot.class)
                .isInstanceOf(DefaultLongPollingUpdateConsumer.class);
        assertThat(service.getUpdatesConsumer()).isSameAs(service);
        assertThat(service.getBotToken()).isEqualTo("bot-token");
    }

    @Test
    void remainsInjectableByBothInterfacesWhenJdkProxied() {
        Object proxy = new ProxyFactory(service).getProxy();

        assertThat(proxy)
                .isInstanceOf(ChatBotService.class)
                .isInstanceOf(SpringLongPollingBot.class)
                .isNotInstanceOf(TelegramChatBotServiceImpl.class);
    }

    @Test
    void forwardsReceivedUpdateToDispatcher() {
        Update update = new Update();

        service.consume(update);

        assertThat(dispatcher.receivedUpdate).isSameAs(update);
    }

    @Test
    void sendsMessageToTelegramTopic() {
        service.sendMessage("-100123_42", "content");

        SendMessage message = sentMessage.get();
        assertThat(message.getChatId()).isEqualTo("-100123");
        assertThat(message.getMessageThreadId()).isEqualTo(42);
        assertThat(message.getText()).isEqualTo("content");
    }

    private static class RecordingDispatcher extends ChatBotMessageDispatcher {

        private Update receivedUpdate;

        RecordingDispatcher() {
            super(null, Runnable::run);
        }

        @Override
        public void dispatchTelegramUpdate(Update update) {
            receivedUpdate = update;
        }
    }
}

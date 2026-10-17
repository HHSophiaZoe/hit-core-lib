package com.hit.chatbot.autoconfigure;

import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.annotation.ChatBotMessageListener;
import com.hit.chatbot.data.response.CommandHelp;
import com.hit.chatbot.data.response.MessageResponse;
import com.hit.chatbot.telegram.TelegramChatBotServiceImpl;
import com.hit.chatbot.telegram.TelegramUpdatePoller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static org.assertj.core.api.Assertions.assertThat;

class HitChatBotAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HitChatBotAutoConfiguration.class, TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(Listeners.class);

    @Test
    void noBotIsCreatedUntilEnabled() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application).doesNotHaveBean(ChatBotService.class);
            assertThat(application).hasSingleBean(ChatBotMessageDispatcher.class);
        });
    }

    @Test
    void telegramBotAndListenersAreWired() {
        context.withPropertyValues(
                        "chatbot.telegram.enable=true",
                        "chatbot.telegram.token=test-token",
                        "chatbot.telegram.base-url=http://127.0.0.1:9",
                        "chatbot.telegram.error-backoff=1h",
                        "commands.status=/status")
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    assertThat(application.getBean(ChatBotService.class)).isInstanceOf(TelegramChatBotServiceImpl.class);
                    assertThat(application).hasSingleBean(TelegramUpdatePoller.class);
                    assertThat(application.getBean(TelegramUpdatePoller.class).isRunning()).isTrue();
                    assertThat(application.getBean(ChatBotMessageDispatcher.class)
                            .getCommandHelps(ChatBotMessageListener.Platform.TELEGRAM, "-100"))
                            .containsExactly(new CommandHelp("/status", "Trạng thái"));
                });
    }

    @Test
    void listenerCreatedWhileAnotherInitializesIsRegisteredToo() {
        // OuterListener gets the lazy InnerListener in its init callback, so Spring creates InnerListener between the
        // before- and after-initialization of OuterListener
        context.withUserConfiguration(NestedListeners.class).run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(ChatBotMessageDispatcher.class)
                    .getCommandHelps(ChatBotMessageListener.Platform.TELEGRAM, "-100"))
                    .extracting(CommandHelp::command)
                    .containsExactlyInAnyOrder("/status", "/outer", "/inner");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class NestedListeners {

        @Bean
        @Lazy
        InnerListener innerListener() {
            return new InnerListener();
        }

        @Bean
        OuterListener outerListener(ObjectProvider<InnerListener> inner) {
            return new OuterListener(inner);
        }
    }

    static class InnerListener {

        @ChatBotMessageListener(platforms = ChatBotMessageListener.Platform.TELEGRAM, commands = "/inner")
        public void inner(MessageResponse message) {
        }
    }

    static class OuterListener implements InitializingBean {

        private final ObjectProvider<InnerListener> inner;

        OuterListener(ObjectProvider<InnerListener> inner) {
            this.inner = inner;
        }

        @Override
        public void afterPropertiesSet() {
            inner.getObject();
        }

        @ChatBotMessageListener(platforms = ChatBotMessageListener.Platform.TELEGRAM, commands = "/outer")
        public void outer(MessageResponse message) {
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Listeners {

        @Bean
        StatusListener statusListener() {
            return new StatusListener();
        }
    }

    static class StatusListener {

        @ChatBotMessageListener(platforms = ChatBotMessageListener.Platform.TELEGRAM, commands = "${commands.status:/status}",
                description = "Trạng thái")
        public void status(MessageResponse message) {
        }
    }
}

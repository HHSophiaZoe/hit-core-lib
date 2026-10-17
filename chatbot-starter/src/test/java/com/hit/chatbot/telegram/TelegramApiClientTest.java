package com.hit.chatbot.telegram;

import com.hit.chatbot.telegram.api.TelegramApiClient;
import com.hit.chatbot.telegram.api.TelegramApiException;
import com.hit.chatbot.telegram.api.TelegramMessage;
import com.hit.chatbot.telegram.api.TelegramSendMessage;
import com.hit.chatbot.telegram.api.TelegramUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramApiClientTest {

    private FakeTelegramServer server;
    private TelegramApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeTelegramServer();
        client = new TelegramApiClient(server.baseUrl(), "123:abc");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void parsesUpdatesAndIgnoresUnknownFields() {
        server.enqueue("getUpdates", """
                {"ok":true,"result":[
                  {"update_id":7,"message":{"message_id":1,"message_thread_id":42,"is_topic_message":true,
                    "chat":{"id":-100123,"type":"supergroup","title":"x"},"text":"/help","date":1700000000,
                    "from":{"id":9,"is_bot":false,"first_name":"a"}}},
                  {"update_id":8,"edited_message":{"message_id":2}}
                ]}""");

        List<TelegramUpdate> updates = client.getUpdates(5, Duration.ofSeconds(1));

        assertThat(updates).hasSize(2);
        TelegramMessage message = updates.getFirst().message();
        assertThat(updates.getFirst().updateId()).isEqualTo(7);
        assertThat(message.messageId()).isEqualTo(1);
        assertThat(message.messageThreadId()).isEqualTo(42);
        assertThat(message.isTopicMessage()).isTrue();
        assertThat(message.chat().id()).isEqualTo(-100123);
        assertThat(message.text()).isEqualTo("/help");
        assertThat(updates.get(1).message()).isNull();

        FakeTelegramServer.Request request = server.requests().getFirst();
        assertThat(request.path()).isEqualTo("/bot123:abc/getUpdates");
        assertThat(request.body())
                .contains("\"offset\":5")
                .contains("\"timeout\":1")
                .contains("\"allowed_updates\":[\"message\"]");
    }

    @Test
    void sendsSnakeCaseBodyWithoutNullFields() {
        server.enqueue("sendMessage", """
                {"ok":true,"result":{"message_id":3,"chat":{"id":-100123},"text":"hi"}}""");

        TelegramMessage sent = client.sendMessage(TelegramSendMessage.builder()
                .chatId("-100123")
                .messageThreadId(42)
                .text("hi")
                .parseMode("HTML")
                .build());

        assertThat(sent.messageId()).isEqualTo(3);
        FakeTelegramServer.Request request = server.requests().getFirst();
        assertThat(request.path()).isEqualTo("/bot123:abc/sendMessage");
        assertThat(request.body())
                .contains("\"chat_id\":\"-100123\"")
                .contains("\"message_thread_id\":42")
                .contains("\"text\":\"hi\"")
                .contains("\"parse_mode\":\"HTML\"")
                .doesNotContain("disable_notification")
                .doesNotContain("protect_content");
    }

    @Test
    void throwsWhenTelegramRejectsCall() {
        server.enqueue("sendMessage", """
                {"ok":false,"error_code":400,"description":"Bad Request: chat not found"}""");

        assertThatThrownBy(() -> client.sendMessage(TelegramSendMessage.builder().chatId("1").text("x").build()))
                .isInstanceOf(TelegramApiException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("chat not found");
    }

    @Test
    void wrapsConnectionFailure() {
        server.close();

        assertThatThrownBy(() -> client.getUpdates(0, Duration.ofSeconds(1)))
                .isInstanceOf(TelegramApiException.class);
    }
}

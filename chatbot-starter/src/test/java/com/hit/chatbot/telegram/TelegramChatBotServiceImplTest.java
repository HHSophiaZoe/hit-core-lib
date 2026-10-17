package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotService;
import com.hit.chatbot.data.request.MessageRequest;
import com.hit.chatbot.telegram.api.TelegramApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramChatBotServiceImplTest {

    private static final String SENT = """
            {"ok":true,"result":{"message_id":1,"chat":{"id":-100123}}}""";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private FakeTelegramServer server;
    private final List<Runnable> executed = new CopyOnWriteArrayList<>();
    private TelegramChatBotServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeTelegramServer();
        service = new TelegramChatBotServiceImpl(new TelegramApiClient(server.baseUrl(), "t", Duration.ofSeconds(2)),
                task -> {
                    executed.add(task);
                    task.run();
                });
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void remainsInjectableByInterfaceWhenJdkProxied() {
        Object proxy = new ProxyFactory(service).getProxy();

        assertThat(proxy)
                .isInstanceOf(ChatBotService.class)
                .isNotInstanceOf(TelegramChatBotServiceImpl.class);
    }

    @Test
    void sendsMessageToTelegramTopic() {
        server.enqueue("sendMessage", SENT);

        service.sendMessage("-100123_42", "content");

        assertThat(sent(0).path("chat_id").asString()).isEqualTo("-100123");
        assertThat(sent(0).path("message_thread_id").asInt()).isEqualTo(42);
        assertThat(sent(0).path("text").asString()).isEqualTo("content");
    }

    @Test
    void plainMessageIsSentAsTextWithoutParseMode() {
        server.enqueue("sendMessage", SENT);

        service.sendMessage("-100123", "a < b & c");

        assertThat(sent(0).has("message_thread_id")).isFalse();
        assertThat(sent(0).has("parse_mode")).isFalse();
        assertThat(sent(0).path("text").asString()).isEqualTo("a < b & c");
    }

    @Test
    void requestIsSentAsHtmlWithEscapedTitle() {
        server.enqueue("sendMessage", SENT);

        service.sendMessage(MessageRequest.builder()
                .chatId("-100123")
                .title("A<B")
                .content("<b>x</b> 1 < 2")
                .build());

        JsonNode body = sent(0);
        assertThat(body.path("parse_mode").asString()).isEqualTo("HTML");
        assertThat(body.path("text").asString()).isEqualTo("<b>A&lt;B</b>\n───────────────\n<b>x</b> 1 &lt; 2");
    }

    @Test
    void anyMessageRequestIsSentAsHtml() {
        server.enqueue("sendMessage", SENT);

        service.sendMessage(MessageRequest.builder().chatId("1").title("Report").content("done").build());

        assertThat(sent(0).path("parse_mode").asString()).isEqualTo("HTML");
        assertThat(sent(0).path("text").asString()).isEqualTo("<b>Report</b>\n───────────────\ndone");
    }

    @Test
    void longTextIsSentAsSeveralMessages() {
        String line = "x".repeat(99) + "\n";
        String text = line.repeat(100);
        this.answerSent(3);

        service.sendMessage("1", text);

        assertThat(server.requests()).hasSize(3);
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            String part = sent(i).path("text").asString();
            assertThat(part.length()).isLessThanOrEqualTo(TelegramChatBotServiceImpl.MAX_MESSAGE_LENGTH);
            joined.append(part);
        }
        assertThat(joined.toString()).isEqualTo(text);
    }

    @Test
    void longHtmlKeepsTagsValidInEveryMessage() {
        String content = "<b>" + "chữ đậm\n".repeat(1000) + "</b>";
        this.answerSent(5);

        service.sendMessage(MessageRequest.builder().chatId("1").content(content).build());

        assertThat(server.requests()).hasSizeGreaterThan(1);
        for (int i = 0; i < server.requests().size(); i++) {
            String part = sent(i).path("text").asString();
            assertThat(part).as("part %d", i).startsWith("<b>").endsWith("</b>");
            assertThat(part.length()).isLessThanOrEqualTo(TelegramChatBotServiceImpl.MAX_MESSAGE_LENGTH);
        }
    }

    @Test
    void rateLimitedMessageIsSentAgainAfterTheWaitTelegramAsks() {
        server.enqueue("sendMessage", """
                {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 1","parameters":{"retry_after":1}}""");
        server.enqueue("sendMessage", SENT);
        long start = System.nanoTime();

        service.sendMessage("1", "x");

        assertThat(server.requests()).hasSize(2);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(900));
    }

    @Test
    void rateLimitLongerThanTheMaximumWaitDropsTheMessage() {
        server.enqueue("sendMessage", """
                {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 60","parameters":{"retry_after":60}}""");

        service.sendMessage("1", "x");

        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void privateMessageGoesToTheUserChat() {
        server.enqueue("sendMessage", SENT);

        service.sendPrivateMessage("777", "hello");

        assertThat(sent(0).path("chat_id").asString()).isEqualTo("777");
        assertThat(sent(0).path("text").asString()).isEqualTo("hello");
    }

    @Test
    void asyncSendRunsOnTheTaskExecutor() {
        server.enqueue("sendMessage", SENT);

        service.sendMessageAsync("1", "x");

        assertThat(executed).hasSize(1);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void swallowsTelegramErrorSoCallerIsNotBroken() {
        server.enqueue("sendMessage", """
                {"ok":false,"error_code":400,"description":"Bad Request"}""");

        service.sendMessage("1", "x");
        service.sendMessage(MessageRequest.builder().chatId("1").content("x").build());

        assertThat(server.requests()).hasSize(2);
    }

    private void answerSent(int times) {
        for (int i = 0; i < times; i++) {
            server.enqueue("sendMessage", SENT);
        }
    }

    private JsonNode sent(int index) {
        return JSON.readTree(server.requests().get(index).body());
    }
}

package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.telegram.api.TelegramApiClient;
import com.hit.chatbot.telegram.api.TelegramUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TelegramUpdatePollerTest {

    private FakeTelegramServer server;
    private RecordingDispatcher dispatcher;
    private TelegramUpdatePoller poller;

    @BeforeEach
    void setUp() throws Exception {
        server = new FakeTelegramServer();
        dispatcher = new RecordingDispatcher();
        poller = new TelegramUpdatePoller(new TelegramApiClient(server.baseUrl(), "t"), dispatcher,
                Duration.ofSeconds(1), Duration.ofMillis(50));
    }

    @AfterEach
    void tearDown() {
        poller.stop();
        server.close();
    }

    @Test
    void advancesOffsetPastLastUpdateAndDispatchesEach() {
        server.enqueue("getUpdates", """
                {"ok":true,"result":[{"update_id":10,"message":{"message_id":1,"chat":{"id":1},"text":"a"}},
                                     {"update_id":11,"message":{"message_id":2,"chat":{"id":1},"text":"b"}}]}""");

        poller.pollOnce();
        poller.pollOnce();

        assertThat(dispatcher.updates).extracting(TelegramUpdate::updateId).containsExactly(10L, 11L);
        assertThat(poller.getOffset()).isEqualTo(12);
        assertThat(server.requests().get(0).body()).contains("\"offset\":0");
        assertThat(server.requests().get(1).body()).contains("\"offset\":12");
    }

    @Test
    void listenerFailureDoesNotBlockLaterUpdates() {
        dispatcher.failOn = 20L;
        server.enqueue("getUpdates", """
                {"ok":true,"result":[{"update_id":20,"message":{"message_id":1,"chat":{"id":1},"text":"a"}},
                                     {"update_id":21,"message":{"message_id":2,"chat":{"id":1},"text":"b"}}]}""");

        poller.pollOnce();

        assertThat(dispatcher.updates).extracting(TelegramUpdate::updateId).containsExactly(20L, 21L);
        assertThat(poller.getOffset()).isEqualTo(22);
    }

    @Test
    void keepsPollingAfterApiErrorAndStopsOnLifecycleStop() {
        server.enqueue("getUpdates", """
                {"ok":false,"error_code":409,"description":"Conflict"}""");
        server.enqueue("getUpdates", """
                {"ok":true,"result":[{"update_id":30,"message":{"message_id":1,"chat":{"id":1},"text":"a"}}]}""");

        poller.start();

        await().atMost(Duration.ofSeconds(5)).until(() -> !dispatcher.updates.isEmpty());
        assertThat(poller.isRunning()).isTrue();
        poller.stop();
        assertThat(poller.isRunning()).isFalse();
        assertThat(dispatcher.updates).extracting(TelegramUpdate::updateId).containsExactly(30L);
    }

    private static class RecordingDispatcher extends ChatBotMessageDispatcher {

        private final List<TelegramUpdate> updates = new CopyOnWriteArrayList<>();
        private Long failOn;

        RecordingDispatcher() {
            super(null, Runnable::run);
        }

        @Override
        public void dispatchTelegramUpdate(TelegramUpdate update) {
            updates.add(update);
            if (failOn != null && failOn == update.updateId()) {
                throw new IllegalStateException("listener failed");
            }
        }
    }
}

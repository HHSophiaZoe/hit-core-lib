package com.hit.chatbot.telegram;

import com.hit.chatbot.ChatBotMessageDispatcher;
import com.hit.chatbot.telegram.api.TelegramApiClient;
import com.hit.chatbot.telegram.api.TelegramUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.List;

/**
 * Long polling {@code getUpdates} trên một thread riêng, chạy theo vòng đời của Spring context.
 */
@Slf4j
public class TelegramUpdatePoller implements SmartLifecycle {

    private final TelegramApiClient client;
    private final ChatBotMessageDispatcher dispatcher;
    private final Duration pollTimeout;
    private final Duration errorBackoff;

    private volatile boolean running;
    private volatile Thread thread;
    private long offset;

    public TelegramUpdatePoller(TelegramApiClient client, ChatBotMessageDispatcher dispatcher,
                                Duration pollTimeout, Duration errorBackoff) {
        this.client = client;
        this.dispatcher = dispatcher;
        this.pollTimeout = pollTimeout;
        this.errorBackoff = errorBackoff;
    }

    @Override
    public void start() {
        running = true;
        thread = Thread.ofPlatform().name("telegram-poller").daemon().start(this::pollLoop);
    }

    @Override
    public void stop() {
        running = false;
        Thread current = thread;
        if (current == null) {
            return;
        }
        current.interrupt();
        try {
            current.join(Duration.ofSeconds(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void pollLoop() {
        while (running) {
            try {
                this.pollOnce();
            } catch (Exception e) {
                if (!running) {
                    return;
                }
                log.error("Telegram polling failed, retry after {}", errorBackoff, e);
                try {
                    Thread.sleep(errorBackoff);
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
        }
    }

    void pollOnce() {
        List<TelegramUpdate> updates = client.getUpdates(offset, pollTimeout);
        for (TelegramUpdate update : updates) {
            offset = update.updateId() + 1;
            try {
                dispatcher.dispatchTelegramUpdate(update);
            } catch (Exception e) {
                log.error("Failed to dispatch Telegram update {}", update.updateId(), e);
            }
        }
    }

    long getOffset() {
        return offset;
    }
}

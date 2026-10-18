package com.hit.websocket.client.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RecentMessagesTest {

    private static final long WINDOW = Duration.ofSeconds(10).toNanos();

    private final RecentMessages recent = new RecentMessages(WINDOW);

    @Test
    void sameMessageInsideWindowIsDuplicate() {
        assertThat(recent.add("{\"a\":1}", 0)).isTrue();
        assertThat(recent.add("{\"a\":2}", 1)).isTrue();
        assertThat(recent.add("{\"a\":1}", 2)).isFalse();
    }

    @Test
    void messageIsForgottenAfterWindow() {
        recent.add("{\"a\":1}", 0);

        assertThat(recent.add("{\"a\":1}", WINDOW + 1)).isTrue();
    }

    @Test
    void clearForgetsEverything() {
        recent.add("{\"a\":1}", 0);
        recent.clear();

        assertThat(recent.add("{\"a\":1}", 1)).isTrue();
    }
}

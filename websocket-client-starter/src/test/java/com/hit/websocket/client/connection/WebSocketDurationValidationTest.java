package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.WebSocketConnectRequest;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketDurationValidationTest {
    @Test
    void rejectsUnrepresentableDurationsWithClearValidationErrors() {
        Duration overflow = Duration.ofNanos(Long.MAX_VALUE).plusNanos(1);
        assertThatThrownBy(() -> new HeartbeatOptions(overflow, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nanosecond");
        assertThatThrownBy(() -> ReconnectPolicy.unlimited(Duration.ZERO, overflow))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nanosecond");
        assertThatThrownBy(() -> WebSocketConnectRequest.of(URI.create("ws://example.test"), overflow))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nanosecond");
    }

    @Test
    void acceptsTheMaximumRepresentableDuration() {
        Duration maximum = Duration.ofNanos(Long.MAX_VALUE);
        assertThatCode(() -> new HeartbeatOptions(maximum, maximum)).doesNotThrowAnyException();
        assertThatCode(() -> ReconnectPolicy.unlimited(Duration.ZERO, maximum)).doesNotThrowAnyException();
        assertThatCode(() -> WebSocketConnectRequest.of(URI.create("ws://example.test"), maximum)).doesNotThrowAnyException();
    }
}

package com.hit.websocket.client.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconnectPolicyTest {

    @Test
    void capsExponentialDelayWithoutOverflow() {
        ReconnectPolicy policy = new ReconnectPolicy(
                -1, Duration.ofSeconds(1), Duration.ofSeconds(30), 0);

        assertThat(policy.delayFor(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.delayFor(5)).isEqualTo(Duration.ofSeconds(16));
        assertThat(policy.delayFor(100)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void zeroAttemptsDisablesRetry() {
        ReconnectPolicy policy = ReconnectPolicy.disabled();

        assertThat(policy.allows(1)).isFalse();
    }

    @Test
    void rejectsAmbiguousNegativeAttemptValues() {
        assertThatThrownBy(() -> new ReconnectPolicy(
                -2, Duration.ZERO, Duration.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

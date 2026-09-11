package com.hit.websocket.client.connection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ConnectionIdTest {

    @Test
    void parsesExternalValue() {
        ConnectionId connectionId = ConnectionId.parse("dnse:market-data-01");

        assertThat(connectionId.provider()).isEqualTo("dnse");
        assertThat(connectionId.name()).isEqualTo("market-data-01");
        assertThat(connectionId.value()).isEqualTo("dnse:market-data-01");
    }

    @Test
    void keepsAdditionalSeparatorsInsideConnectionName() {
        ConnectionId connectionId = ConnectionId.parse("provider:region:prices");

        assertThat(connectionId.provider()).isEqualTo("provider");
        assertThat(connectionId.name()).isEqualTo("region:prices");
    }

    @Test
    void rejectsInvalidExternalValue() {
        assertThatNullPointerException().isThrownBy(() -> ConnectionId.parse(null));
        assertThatIllegalArgumentException().isThrownBy(() -> ConnectionId.parse("missing-separator"));
        assertThatIllegalArgumentException().isThrownBy(() -> ConnectionId.parse(":missing-source"));
        assertThatIllegalArgumentException().isThrownBy(() -> ConnectionId.parse("missing-name:"));
    }
}

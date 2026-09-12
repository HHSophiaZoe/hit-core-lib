package com.hit.websocket.client.registration;

import com.hit.websocket.client.connection.ConnectionId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultWebSocketRegistrationTest {

    @Test
    void supportsClientWithoutConnectionAssignment() {
        DefaultWebSocketRegistration registration = new DefaultWebSocketRegistration(() -> { });

        assertThat(registration.connectionIds()).isEmpty();
        assertThat(registration.isClosed()).isFalse();
    }

    @Test
    void supportsSingleConnectionClient() {
        ConnectionId connectionId = new ConnectionId("market", "primary");
        DefaultWebSocketRegistration registration = new DefaultWebSocketRegistration(connectionId, () -> { });

        assertThat(registration.connectionIds()).containsExactly(connectionId);
    }

    @Test
    void supportsConnectionAssignmentFromPool() {
        ConnectionId connectionId = new ConnectionId("market", "primary");
        Set<ConnectionId> connections = new HashSet<>(Set.of(connectionId));
        DefaultWebSocketRegistration registration = new DefaultWebSocketRegistration(() -> { });

        registration.assignConnections(connections);
        connections.clear();

        assertThat(registration.connectionIds()).containsExactly(connectionId);
    }

    @Test
    void closesOnlyOnce() {
        AtomicInteger closeCount = new AtomicInteger();
        DefaultWebSocketRegistration registration = new DefaultWebSocketRegistration(closeCount::incrementAndGet);

        registration.close();
        registration.close();

        assertThat(closeCount).hasValue(1);
        assertThat(registration.isClosed()).isTrue();
    }
}

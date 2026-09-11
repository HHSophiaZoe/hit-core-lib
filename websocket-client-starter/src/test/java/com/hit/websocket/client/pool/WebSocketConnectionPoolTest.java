package com.hit.websocket.client.pool;

import com.hit.websocket.client.connection.ConnectionId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class WebSocketConnectionPoolTest {

    private final Map<ConnectionId, TestConnection> connections = new LinkedHashMap<>();
    private WebSocketConnectionPool<String, TestConnection> pool;

    @BeforeEach
    void setup() {
        WebSocketConnectionPoolFactory factory = new WebSocketConnectionPoolFactory(
                Thread.ofVirtual().name("pool-test-", 0).factory());
        pool = factory.create(new WebSocketConnectionPoolOptions(3, 7), this::connection);
    }

    @AfterEach
    void cleanup() {
        pool.close();
    }

    @Test
    void fillsExistingConnectionsBeforeCreatingAnotherConnection() {
        PoolAllocation<String> first = pool.acquire(List.of("a", "b", "c", "d"));
        PoolAllocation<String> second = pool.acquire(List.of("e", "f", "g"));

        assertThat(first.connectionIds()).extracting(ConnectionId::name).containsExactlyInAnyOrder("01", "02");
        assertThat(second.assigned().keySet()).extracting(ConnectionId::name).containsExactlyInAnyOrder("02", "03");
        assertThat(pool.resources(new ConnectionId("test", "02"))).containsExactly("d", "e", "f");
        assertThat(connections).hasSize(3);
    }

    @Test
    void referenceCountPreventsPrematureRelease() {
        pool.acquire(List.of("a"));
        pool.acquire(List.of("a"));

        assertThat(pool.release(List.of("a")).removed()).isEmpty();
        PoolRelease<String> release = pool.release(List.of("a"));

        assertThat(release.removed()).containsEntry(new ConnectionId("test", "01"), List.of("a"));
        assertThat(release.unusedConnections()).containsExactly(new ConnectionId("test", "01"));
        assertThat(pool.isConnected()).isFalse();
    }

    @Test
    void validatesTotalCapacityBeforeChangingAssignments() {
        pool.acquire(List.of("a", "b", "c", "d", "e", "f"));

        assertThatThrownBy(() -> pool.acquire(List.of("g", "h")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 additional resources")
                .hasMessageContaining("1/7 remain");
        assertThat(connections).hasSize(2);
    }

    @Test
    void delegatesConnectionLifecycleWithoutLocks() {
        PoolAllocation<String> allocation = pool.acquire(List.of("a"));
        pool.connect(allocation.connectionIds());
        pool.maintainConnections();

        assertThat(pool.isConnected()).isTrue();
        assertThat(connections.get(new ConnectionId("test", "01")).maintained).isTrue();

        pool.disconnect(allocation.connectionIds());
        assertThat(pool.isConnected()).isFalse();
    }

    @Test
    void connectionCallbackCanReadPoolState() {
        PoolAllocation<String> allocation = pool.acquire(List.of("a"));
        ConnectionId connectionId = allocation.connectionIds().iterator().next();
        connections.get(connectionId).onConnect = () -> assertThat(pool.resources(connectionId)).containsExactly("a");

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> pool.connect(allocation.connectionIds()));
    }

    @Test
    void usesConfiguredVirtualCoordinator() {
        pool.acquire(List.of("a"));

        assertThat(connections.get(new ConnectionId("test", "01")).createdByVirtualThread).isTrue();
    }

    private TestConnection connection(int sequence) {
        TestConnection connection = new TestConnection(new ConnectionId("test", "%02d".formatted(sequence)));
        connections.put(connection.connectionId(), connection);
        return connection;
    }

    private static final class TestConnection implements PooledWebSocketConnection {
        private final ConnectionId connectionId;
        private final boolean createdByVirtualThread;
        private boolean connected;
        private boolean maintained;
        private Runnable onConnect = () -> { };

        private TestConnection(ConnectionId connectionId) {
            this.connectionId = connectionId;
            this.createdByVirtualThread = Thread.currentThread().isVirtual();
        }

        @Override
        public ConnectionId connectionId() {
            return connectionId;
        }

        @Override
        public void connect() {
            connected = true;
            onConnect.run();
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void maintain() {
            maintained = true;
        }

        @Override
        public void close() {
            connected = false;
        }
    }
}

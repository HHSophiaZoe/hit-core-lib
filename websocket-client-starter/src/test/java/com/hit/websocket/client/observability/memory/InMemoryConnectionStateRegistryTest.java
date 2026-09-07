package com.hit.websocket.client.observability.memory;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.connection.ConnectionState;
import com.hit.websocket.client.observability.model.ConnectionDailySnapshot;
import com.hit.websocket.client.observability.model.ConnectionEvent;
import com.hit.websocket.client.observability.model.ConnectionEventDetails;
import com.hit.websocket.client.observability.model.ConnectionEventType;
import com.hit.websocket.client.observability.model.ConnectionSnapshot;
import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.FailureCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryConnectionStateRegistryTest {

    private final ConnectionId connectionId = new ConnectionId("dnse", "market-data");
    private final InMemoryConnectionStateRegistry registry = new InMemoryConnectionStateRegistry();

    @Test
    void ignoresEventsAndTrafficFromStaleConnectionGeneration() {
        registry.onEvent(event(1, 1, ConnectionEventType.TRANSPORT_CONNECTED,
                ConnectionState.TRANSPORT_CONNECTED));
        registry.onEvent(event(2, 1, ConnectionEventType.CONNECT_REQUESTED,
                ConnectionState.CONNECTING));
        registry.onEvent(event(1, 2, ConnectionEventType.TRANSPORT_CLOSED,
                ConnectionState.RETRY_WAIT));
        registry.onMessageReceived(connectionId, 1, 100);
        registry.onMessageReceived(connectionId, 2, 50);

        ConnectionSnapshot snapshot = registry.find(connectionId).orElseThrow();
        assertThat(snapshot.generation()).isEqualTo(2);
        assertThat(snapshot.state()).isEqualTo(ConnectionState.CONNECTING);
        assertThat(snapshot.receivedMessages()).isEqualTo(1);
        assertThat(snapshot.receivedBytes()).isEqualTo(50);
    }

    @Test
    void capturesSanitizedErrorForDashboardSnapshot() {
        registry.onEvent(new ConnectionEvent(
                connectionId, 1, 1, ConnectionEventType.ERROR, ConnectionState.RETRY_WAIT,
                Instant.parse("2026-09-06T00:00:00Z"),
                new ConnectionEventDetails.Failure(
                        FailureCategory.NETWORK, "PONG_TIMEOUT", "No pong", true, true)));

        ConnectionSnapshot snapshot = registry.find(connectionId).orElseThrow();
        assertThat(snapshot.lastErrorCategory()).isEqualTo("network");
        assertThat(snapshot.lastErrorCode()).isEqualTo("PONG_TIMEOUT");
        assertThat(snapshot.lastErrorMessage()).isEqualTo("No pong");
    }

    @Test
    void countsDailyConnectionLossRetryAndTraffic() {
        registry.onEvent(new ConnectionEvent(connectionId, 1, 1,
                ConnectionEventType.CONNECT_REQUESTED, ConnectionState.CONNECTING,
                Instant.now(), new ConnectionEventDetails.ConnectAttempt(0)));
        registry.onEvent(new ConnectionEvent(connectionId, 1, 2,
                ConnectionEventType.TRANSPORT_CLOSED, ConnectionState.CONNECTING,
                Instant.now(), new ConnectionEventDetails.TransportClosed(
                        new CloseReason(1006, "closed", false))));
        registry.onEvent(new ConnectionEvent(connectionId, 1, 3,
                ConnectionEventType.RETRY_SCHEDULED, ConnectionState.RETRY_WAIT,
                Instant.now(), new ConnectionEventDetails.RetryScheduled(
                        1, 3, java.time.Duration.ofSeconds(1))));
        registry.onMessageReceived(connectionId, 1, 42);

        ConnectionDailySnapshot today = registry.find(connectionId).orElseThrow().today();
        assertThat(today.connectAttempts()).isEqualTo(1);
        assertThat(today.connectionLosses()).isEqualTo(1);
        assertThat(today.retries()).isEqualTo(1);
        assertThat(today.receivedMessages()).isEqualTo(1);
        assertThat(today.receivedBytes()).isEqualTo(42);
    }

    private ConnectionEvent event(
            long generation,
            long sequence,
            ConnectionEventType type,
            ConnectionState state
    ) {
        return new ConnectionEvent(
                connectionId, generation, sequence, type, state, Instant.now(), details(type));
    }

    private ConnectionEventDetails details(ConnectionEventType type) {
        return type == ConnectionEventType.TRANSPORT_CLOSED
                ? new ConnectionEventDetails.TransportClosed(new CloseReason(1006, "closed", false))
                : type == ConnectionEventType.CONNECT_REQUESTED
                ? new ConnectionEventDetails.ConnectAttempt(0)
                : ConnectionEventDetails.None.INSTANCE;
    }
}

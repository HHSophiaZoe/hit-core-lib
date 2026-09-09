package com.hit.websocket.client.dispatch.memory;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.dispatch.DispatchContext;
import com.hit.websocket.client.dispatch.model.DispatcherSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDispatcherStateRegistryTest {

    @Test
    void isolatesDispatchersWithTheSameNameByConnection() {
        InMemoryDispatcherStateRegistry registry = new InMemoryDispatcherStateRegistry(
                Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC));
        ConnectionId prices = new ConnectionId("dnse", "prices");
        ConnectionId orders = new ConnectionId("dnse", "orders");
        registry.queueRegistered(prices, "trade", 0, () -> 3);
        registry.queueRegistered(orders, "trade", 0, () -> 7);
        registry.processed(new DispatchContext(prices, "trade", "trade", 0), Duration.ofMillis(4));
        registry.failed(new DispatchContext(orders, "trade", "trade", 0), new IllegalStateException("failed"));

        DispatcherSnapshot priceSnapshot = registry.findByConnection(prices).getFirst();
        DispatcherSnapshot orderSnapshot = registry.findByConnection(orders).getFirst();

        assertThat(priceSnapshot.queued()).isEqualTo(3);
        assertThat(priceSnapshot.processed()).isEqualTo(1);
        assertThat(priceSnapshot.errors()).isZero();
        assertThat(orderSnapshot.queued()).isEqualTo(7);
        assertThat(orderSnapshot.processed()).isZero();
        assertThat(orderSnapshot.errors()).isEqualTo(1);
    }
}

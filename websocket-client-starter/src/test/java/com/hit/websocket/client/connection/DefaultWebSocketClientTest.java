package com.hit.websocket.client.connection;

import com.hit.websocket.client.observability.memory.InMemoryConnectionStateRegistry;
import com.hit.websocket.client.observability.model.ConnectionEventType;
import com.hit.websocket.client.observability.model.ConnectionSnapshot;
import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.FailureCategory;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketConnectRequest;
import com.hit.websocket.client.transport.WebSocketFrame;
import com.hit.websocket.client.transport.WebSocketTransport;
import com.hit.websocket.client.transport.WebSocketTransportListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class DefaultWebSocketClientTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-07T18:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final InMemoryConnectionStateRegistry snapshots = new InMemoryConnectionStateRegistry(clock, 100);
    private final List<FakeTransport> transports = new CopyOnWriteArrayList<>();
    private Supplier<FakeTransport> transportSupplier = FakeTransport::new;
    private final WebSocketClientFactory factory = new WebSocketClientFactory(id -> {
        FakeTransport transport = transportSupplier.get();
        transports.add(transport);
        return transport;
    }, List.of(snapshots), clock);
    private final ConnectionId id = new ConnectionId("test", "quotes");

    @AfterEach
    void shutdown() {
        factory.close();
        await().untilAsserted(() -> assertThat(transports).allMatch(transport -> !transport.open));
    }

    @Test
    void sendCompletionReportsHeartbeatAndRejectedSend() {
        List<Boolean> outcomes = new CopyOnWriteArrayList<>();
        List<WebSocketFrame> frames = new CopyOnWriteArrayList<>();
        transportSupplier = () -> { FakeTransport transport = new FakeTransport(); transport.echoPong = true; return transport; };
        WebSocketClient client = create(options().heartbeat(new HeartbeatOptions(Duration.ofMillis(20), Duration.ofSeconds(1))).build(),
                new WebSocketClientListener() {
                    @Override public void onMessage(WebSocketFrame frame) { }
                    @Override public void onSendCompleted(WebSocketFrame frame, boolean accepted) {
                        frames.add(frame);
                        outcomes.add(accepted);
                    }
                });
        client.send(new WebSocketFrame.Text("not connected"));
        await().untilAsserted(() -> assertThat(outcomes).containsExactly(false));
        connected(client);
        await().untilAsserted(() -> assertThat(frames).anyMatch(frame -> frame instanceof WebSocketFrame.Ping));
        assertThat(outcomes).contains(true);
    }

    @Test
    void noAuthConnectsImmediatelyAndUsesInjectedClock() {
        WebSocketClient client = create(options().build(), frame -> { });
        connected(client);
        ConnectionSnapshot snapshot = snapshots.find(id).orElseThrow();
        assertThat(snapshot.connectedAt()).isEqualTo(clock.instant());
        assertThat(snapshot.today().date().toString()).isEqualTo("2026-09-08");
        assertThat(snapshot.today().transportConnections()).isOne();
        assertThat(client.isHealthy()).isTrue();
    }

    @Test
    void authIsApplicationCodeAndCanSendBeforeBusinessReady() {
        AtomicReference<WebSocketClient> reference = new AtomicReference<>();
        CompletableFuture<Void> authenticated = new CompletableFuture<>();
        WebSocketClientListener listener = new WebSocketClientListener() {
            @Override
            public void onTransportConnected() {
                reference.get().send(new WebSocketFrame.Text("auth"));
            }

            @Override
            public void onMessage(WebSocketFrame frame) {
                if (frame.equals(new WebSocketFrame.Text("auth-ok"))) authenticated.complete(null);
            }
        };
        WebSocketClient client = create(options().build(), listener);
        reference.set(client);
        connected(client);
        await().untilAsserted(() -> assertThat(transports.getFirst().sent).containsExactly(new WebSocketFrame.Text("auth")));
        assertThat(authenticated).isNotDone();
        transports.getFirst().frame(new WebSocketFrame.Text("auth-ok"));
        await().until(authenticated::isDone);
    }

    @Test
    void staleCallbacksCannotAffectTheReplacementConnection() {
        List<WebSocketFrame> received = new CopyOnWriteArrayList<>();
        WebSocketClient client = create(options().build(), received::add);
        connected(client);
        FakeTransport old = transports.getFirst();
        client.disconnect();
        await().until(() -> client.state() == ConnectionState.STOPPED);
        connected(client);
        old.frame(new WebSocketFrame.Text("old"));
        old.listener.onFailure(networkFailure());
        transports.getLast().frame(new WebSocketFrame.Text("new"));
        await().untilAsserted(() -> assertThat(received).containsExactly(new WebSocketFrame.Text("new")));
        assertThat(client.isConnected()).isTrue();
        assertThat(transports).hasSize(2);
    }

    @Test
    void errorCallbackCanDisconnectAndReconnectWithoutReentrantTransitions() {
        AtomicReference<WebSocketClient> reference = new AtomicReference<>();
        WebSocketClient client = create(options().build(), new WebSocketClientListener() {
            @Override public void onMessage(WebSocketFrame frame) { }
            @Override public void onError(TransportFailure error) {
                reference.get().disconnect();
                reference.get().connect();
            }
        });
        reference.set(client);
        connected(client);
        client.fail(networkFailure());
        await().until(() -> transports.size() == 2 && client.isConnected());
        assertThat(transports.getFirst().open).isFalse();
    }

    @Test
    void normalRemoteCloseReconnectsWhileExplicitDisconnectStopsRetries() {
        WebSocketClient client = create(options().build(), frame -> { });
        connected(client);
        transports.getFirst().listener.onClosed(new CloseReason(1001, "server restart", false));
        await().until(() -> transports.size() == 2 && client.isConnected());
        client.disconnect();
        await().until(() -> client.state() == ConnectionState.STOPPED);
        await().during(Duration.ofMillis(150)).atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(transports).hasSize(2));
    }

    @Test
    void rapidConnectDropCyclesExhaustRetryBudget() {
        WebSocketClient client = create(options()
                .reconnectPolicy(new ReconnectPolicy(1, Duration.ofMillis(10), Duration.ofMillis(10), 0)).build(), frame -> { });
        connected(client);
        client.fail(networkFailure());
        await().until(() -> transports.size() == 2 && client.isConnected());
        client.fail(networkFailure());
        await().until(() -> client.state() == ConnectionState.FAILED);
        assertThat(transports).hasSize(2);
    }

    @Test
    void nonRetryableFailureKeepsItsOriginalDetails() {
        WebSocketClient client = create(options().build(), frame -> { });
        connected(client);
        client.fail(new TransportFailure(FailureCategory.AUTHENTICATION, "AUTH_REJECTED", "invalid credentials", false));
        await().until(() -> client.state() == ConnectionState.FAILED);
        await().untilAsserted(() -> assertThat(snapshots.find(id).orElseThrow().state()).isEqualTo(ConnectionState.FAILED));
        assertThat(snapshots.find(id).orElseThrow().lastErrorCode()).isEqualTo("AUTH_REJECTED");
        assertThat(transports).hasSize(1);
    }

    @Test
    void connectTimeoutWorksEvenWithAStalledCustomTransport() {
        transportSupplier = () -> {
            FakeTransport transport = new FakeTransport();
            transport.connectResult = new CompletableFuture<>();
            return transport;
        };
        WebSocketClient client = create(options()
                .connectRequest(WebSocketConnectRequest.of(URI.create("ws://example.test"), Duration.ofMillis(30)))
                .reconnectPolicy(ReconnectPolicy.disabled()).build(), frame -> { });
        client.connect();
        await().until(() -> client.state() == ConnectionState.FAILED);
        assertThat(transports.getFirst().open).isFalse();
        assertThat(snapshots.find(id).orElseThrow().lastErrorCode()).isEqualTo("CONNECT_TIMEOUT");
    }

    @Test
    void factoryFailureAlsoFollowsReconnectPolicy() {
        transportSupplier = () -> { throw new IllegalStateException("factory failed"); };
        WebSocketClient client = create(options().reconnectPolicy(ReconnectPolicy.disabled()).build(), frame -> { });
        client.connect();
        await().until(() -> client.state() == ConnectionState.FAILED);
        assertThat(snapshots.find(id).orElseThrow().lastErrorMessage()).isEqualTo("factory failed");
    }

    @Test
    void nativeHeartbeatPongAllowsTheNextPing() {
        transportSupplier = () -> {
            FakeTransport transport = new FakeTransport();
            transport.echoPong = true;
            return transport;
        };
        WebSocketClient client = create(options().heartbeat(new HeartbeatOptions(Duration.ofMillis(20), Duration.ofSeconds(1))).build(), frame -> { });
        connected(client);
        await().until(() -> transports.getFirst().sent.size() >= 3);
        assertThat(client.isHealthy()).isTrue();
        assertThat(transports).hasSize(1);
    }

    @Test
    void missingPongFailsAndCustomHeartbeatMaySkipTicks() {
        WebSocketClient client = create(options()
                .heartbeat(new HeartbeatOptions(Duration.ofMillis(20), Duration.ofMillis(30)))
                .reconnectPolicy(ReconnectPolicy.disabled()).build(), frame -> { });
        client.setHeartbeatFrameSupplier(() -> null);
        connected(client);
        await().during(Duration.ofMillis(100)).atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(transports.getFirst().sent).isEmpty());
        client.setHeartbeatFrameSupplier(() -> new WebSocketFrame.Text("ping"));
        await().until(() -> client.state() == ConnectionState.FAILED);
        assertThat(snapshots.find(id).orElseThrow().lastErrorCode()).isEqualTo("PONG_TIMEOUT");
        assertThat(snapshots.findRecent(id, 100)).anyMatch(event -> event.type() == ConnectionEventType.HEARTBEAT_TIMEOUT);
    }

    @Test
    void inboundOverflowIsBoundedAndFailsInsteadOfGrowingWithoutLimit() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        WebSocketClient client = create(options().inboundCapacity(1).reconnectPolicy(ReconnectPolicy.disabled()).build(), frame -> {
            entered.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });
        connected(client);
        try {
            transports.getFirst().frame(new WebSocketFrame.Text("first"));
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            transports.getFirst().frame(new WebSocketFrame.Text("overflow"));
        } finally {
            release.countDown();
        }
        await().until(() -> client.state() == ConnectionState.FAILED);
        assertThat(snapshots.find(id).orElseThrow().lastErrorCode()).isEqualTo("INBOUND_OVERFLOW");
    }

    @Test
    void closeIsPermanentAndFactoryRejectsCreationAfterShutdown() {
        WebSocketClient client = create(options().build(), frame -> { });
        connected(client);
        client.close();
        await().until(() -> client.state() == ConnectionState.STOPPED);
        assertThatThrownBy(client::connect).isInstanceOf(IllegalStateException.class);
        factory.close();
        assertThatThrownBy(() -> factory.create(options().build(), frame -> { })).isInstanceOf(IllegalStateException.class);
    }

    private WebSocketClient create(WebSocketClientOptions options, WebSocketClientListener listener) {
        return factory.create(options, listener);
    }

    private void connected(WebSocketClient client) {
        client.connect();
        await().until(client::isConnected);
        await().untilAsserted(() -> assertThat(snapshots.find(id).orElseThrow().state()).isEqualTo(ConnectionState.CONNECTED));
    }

    private WebSocketClientOptions.WebSocketClientOptionsBuilder options() {
        return WebSocketClientOptions.builder()
                .connectionId(id)
                .connectRequest(WebSocketConnectRequest.of(URI.create("ws://example.test"), Duration.ofSeconds(1)))
                .reconnectPolicy(new ReconnectPolicy(3, Duration.ofMillis(20), Duration.ofMillis(50), 0));
    }

    private static TransportFailure networkFailure() {
        return new TransportFailure(FailureCategory.NETWORK, "RESET", "connection reset", true);
    }

    private static final class FakeTransport implements WebSocketTransport {
        private volatile boolean open;
        private volatile WebSocketTransportListener listener;
        private CompletableFuture<Void> connectResult = CompletableFuture.completedFuture(null);
        private boolean echoPong;
        private final List<WebSocketFrame> sent = new CopyOnWriteArrayList<>();

        @Override
        public CompletionStage<Void> connect(WebSocketConnectRequest request, WebSocketTransportListener listener) {
            this.listener = listener;
            open = connectResult.isDone();
            return connectResult;
        }

        @Override
        public CompletionStage<Void> send(WebSocketFrame frame) {
            sent.add(frame);
            if (echoPong && frame instanceof WebSocketFrame.Ping ping) listener.onFrame(new WebSocketFrame.Pong(ping.payload()));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> disconnect(CloseReason reason) {
            open = false;
            return CompletableFuture.completedFuture(null);
        }

        @Override public boolean isOpen() { return open; }
        void frame(WebSocketFrame frame) { listener.onFrame(frame); }
    }
}

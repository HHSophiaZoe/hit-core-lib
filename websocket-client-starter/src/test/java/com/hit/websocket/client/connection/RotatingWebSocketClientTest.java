package com.hit.websocket.client.connection;

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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RotatingWebSocketClientTest {

    private final List<FakeTransport> transports = new CopyOnWriteArrayList<>();
    private final WebSocketClientFactory factory = new WebSocketClientFactory(id -> {
        FakeTransport transport = new FakeTransport(id);
        transports.add(transport);
        return transport;
    }, List.of(), Clock.systemUTC());
    private final RecordingListener listener = new RecordingListener();

    @AfterEach
    void shutdown() {
        factory.close();
    }

    @Test
    void firstSessionIsConnectedOnlyAfterAdapterMarksItReady() {
        RotatingWebSocketClient client = create(RotationOptions.every(Duration.ofHours(1)));
        client.connect();

        await().until(() -> listener.opened.size() == 1);
        assertThat(transports.getFirst().id.name()).isEqualTo("quotes#a");
        assertThat(client.isConnected()).isFalse();

        listener.opened.getFirst().markReady();
        await().until(client::isConnected);
        assertThat(listener.connected).hasSize(1);
        assertThat(client.sessions()).hasSize(1);
    }

    @Test
    void rotationOpensNewSessionBeforeClosingOldAndDeliversEachMessageOnce() {
        RotatingWebSocketClient client = create(fastRotation(Duration.ofMillis(100), Duration.ofSeconds(5)));
        connectReady(client);
        FakeTransport old = transports.getFirst();

        await().until(() -> listener.opened.size() == 2);
        FakeTransport next = transports.get(1);
        assertThat(next.id.name()).isEqualTo("quotes#b");
        assertThat(client.sessions()).hasSize(2);

        old.frame("m1");
        next.frame("m1"); // Cùng message nhận từ cả hai session.
        next.frame("m2");
        listener.opened.get(1).markReady();
        old.frame("m2"); // Session cũ còn nhận nốt trong drainDelay.
        old.frame("m3");

        await().untilAsserted(() -> assertThat(listener.messages).containsExactly("m1", "m2", "m3"));
        await().until(() -> !old.open);
        assertThat(next.open).isTrue();
        assertThat(client.sessions()).hasSize(1);
        assertThat(listener.connected).as("xoay theo kế hoạch không báo kết nối lại").hasSize(1);
        assertThat(listener.disconnected).isZero();
    }

    @Test
    void newSessionNotReadyInTimeIsDroppedAndCurrentOneKept() {
        RotatingWebSocketClient client = create(fastRotation(Duration.ofMillis(50), Duration.ofMillis(100)));
        connectReady(client);
        FakeTransport current = transports.getFirst();

        await().until(() -> transports.size() >= 2 && !transports.get(1).open);
        assertThat(current.open).isTrue();
        assertThat(client.isConnected()).isTrue();
        current.frame("still-here");
        await().untilAsserted(() -> assertThat(listener.messages).contains("still-here"));
        assertThat(listener.disconnected).isZero();
    }

    @Test
    void unplannedDropReportsDisconnectedThenConnectedAfterSessionIsReadyAgain() {
        RotatingWebSocketClient client = create(RotationOptions.every(Duration.ofHours(1)));
        connectReady(client);

        transports.getFirst().listener.onFailure(new TransportFailure(FailureCategory.NETWORK, "RESET", "reset", true));

        await().until(() -> listener.disconnected == 1);
        assertThat(client.isConnected()).isFalse();
        await().until(() -> listener.opened.size() == 2); // Cùng session, WebSocketClient tự kết nối lại.
        listener.opened.getLast().markReady();
        await().until(() -> listener.connected.size() == 2);
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    void messagesAreNotDeduplicatedOutsideRotation() {
        RotatingWebSocketClient client = create(RotationOptions.every(Duration.ofHours(1)));
        connectReady(client);

        transports.getFirst().frame("same");
        transports.getFirst().frame("same");

        await().untilAsserted(() -> assertThat(listener.messages).containsExactly("same", "same"));
    }

    private RotatingWebSocketClient create(RotationOptions rotation) {
        WebSocketClientOptions options = WebSocketClientOptions.builder()
                .connectionId(new ConnectionId("test", "quotes"))
                .connectRequest(WebSocketConnectRequest.of(URI.create("ws://example.test"), Duration.ofSeconds(1)))
                .reconnectPolicy(new ReconnectPolicy(-1, Duration.ofMillis(20), Duration.ofMillis(50), 0))
                .build();
        return factory.createRotating(options, rotation, listener);
    }

    private void connectReady(RotatingWebSocketClient client) {
        client.connect();
        await().until(() -> !listener.opened.isEmpty());
        listener.opened.getFirst().markReady();
        await().until(client::isConnected);
    }

    private static RotationOptions fastRotation(Duration interval, Duration readyTimeout) {
        return new RotationOptions(interval, readyTimeout, Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofHours(1));
    }

    private static final class RecordingListener implements RotatingWebSocketListener {

        private final List<RotatingSession> opened = new CopyOnWriteArrayList<>();
        private final List<String> messages = new CopyOnWriteArrayList<>();
        private final List<Boolean> connected = new CopyOnWriteArrayList<>();
        private volatile int disconnected;

        @Override
        public void onSessionOpened(RotatingSession session) {
            opened.add(session);
        }

        @Override
        public void onMessage(RotatingSession session, WebSocketFrame frame) {
            if (frame instanceof WebSocketFrame.Text(String payload)) messages.add(payload);
        }

        @Override
        public void onConnected() {
            connected.add(true);
        }

        @Override
        public void onDisconnected() {
            disconnected++;
        }
    }

    private static final class FakeTransport implements WebSocketTransport {

        private final ConnectionId id;
        private volatile boolean open;
        private volatile WebSocketTransportListener listener;

        private FakeTransport(ConnectionId id) {
            this.id = id;
        }

        @Override
        public CompletionStage<Void> connect(WebSocketConnectRequest request, WebSocketTransportListener listener) {
            this.listener = listener;
            open = true;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> send(WebSocketFrame frame) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> disconnect(CloseReason reason) {
            open = false;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        void frame(String payload) {
            listener.onFrame(new WebSocketFrame.Text(payload));
        }
    }
}

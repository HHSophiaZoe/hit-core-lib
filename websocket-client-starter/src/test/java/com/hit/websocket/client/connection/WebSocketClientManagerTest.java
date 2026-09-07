package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketConnectRequest;
import com.hit.websocket.client.transport.WebSocketFrame;
import com.hit.websocket.client.transport.WebSocketTransport;
import com.hit.websocket.client.transport.WebSocketTransportFactory;
import com.hit.websocket.client.transport.WebSocketTransportListener;
import com.hit.websocket.client.observability.memory.InMemoryConnectionStateRegistry;
import com.hit.websocket.client.observability.model.ConnectionSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketClientManagerTest {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Test
    void oldTerminationCannotStopConnectionStartedByErrorCallback() {
        FakeTransportFactory transports = new FakeTransportFactory();
        java.util.concurrent.atomic.AtomicReference<ManagedWebSocketClient> reference = new java.util.concurrent.atomic.AtomicReference<>();
        WebSocketClientListener listener = new WebSocketClientListener() {
            @Override
            public void onMessage(WebSocketFrame frame) {
            }

            @Override
            public void onError(TransportFailure failure) {
                reference.get().disconnect();
                reference.get().connect();
            }
        };
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(transports, scheduler, List.of()).create(options(), listener);
        reference.set(client);
        client.connect();
        client.fail(TransportFailure.unknown(new IllegalStateException("broken transport")));
        assertThat(transports.created.get()).isEqualTo(2);
        assertThat(client.state()).isEqualTo(ConnectionState.TRANSPORT_CONNECTED);
        client.disconnect();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Test
    void disconnectCancelsReconnectAndIgnoresCallbacksFromOldTransport() throws Exception {
        FakeTransportFactory transports = new FakeTransportFactory();
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(
                transports, scheduler, List.of()).create(options(), new NoOpListener());

        client.connect();
        client.markReady();
        FakeTransport original = transports.latest;
        client.disconnect();
        original.fail(new TransportFailure(
                com.hit.websocket.client.transport.FailureCategory.NETWORK,
                "LATE_FAILURE", "late", true));

        Thread.sleep(50);
        assertThat(transports.created.get()).isEqualTo(1);
        assertThat(client.state()).isEqualTo(ConnectionState.STOPPED);
    }

    @Test
    void applicationFailureClosesTransportAndUsesRetryPolicy() throws Exception {
        FakeTransportFactory transports = new FakeTransportFactory();
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(
                transports, scheduler, List.of()).create(options(), new NoOpListener());

        client.connect();
        client.fail(new TransportFailure(
                com.hit.websocket.client.transport.FailureCategory.AUTHENTICATION,
                "AUTH_FAILED", "authentication rejected", true));

        Thread.sleep(50);
        assertThat(transports.created.get()).isGreaterThanOrEqualTo(2);
        assertThat(client.state()).isEqualTo(ConnectionState.TRANSPORT_CONNECTED);
    }

    @Test
    void disconnectIsIdempotentWhileAlreadyStopped() {
        FakeTransportFactory transports = new FakeTransportFactory();
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(
                transports, scheduler, List.of()).create(options(), new NoOpListener());

        client.disconnect();
        client.disconnect();

        assertThat(transports.created.get()).isZero();
        assertThat(client.state()).isEqualTo(ConnectionState.STOPPED);
    }

    @Test
    void nonRetryableFailureKeepsOriginalErrorInFailedSnapshot() {
        FakeTransportFactory transports = new FakeTransportFactory();
        InMemoryConnectionStateRegistry snapshots = new InMemoryConnectionStateRegistry();
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(
                transports, scheduler, List.of(snapshots)).create(options(), new NoOpListener());

        client.connect();
        client.fail(new TransportFailure(
                com.hit.websocket.client.transport.FailureCategory.AUTHENTICATION,
                "AUTH_REJECTED", "invalid credentials", false));

        ConnectionSnapshot snapshot = snapshots.find(new ConnectionId("dnse", "market-data")).orElseThrow();
        assertThat(snapshot.state()).isEqualTo(ConnectionState.FAILED);
        assertThat(snapshot.lastErrorCode()).isEqualTo("AUTH_REJECTED");
        assertThat(snapshot.lastErrorMessage()).isEqualTo("invalid credentials");
    }

    @Test
    void lifecycleStageTimeoutReconnectsAStalledProtocol() throws Exception {
        FakeTransportFactory transports = new FakeTransportFactory();
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(
                transports, scheduler, List.of()).create(options(Duration.ofMillis(10)), new NoOpListener());

        client.connect();
        Thread.sleep(40);

        assertThat(transports.created.get()).isGreaterThanOrEqualTo(2);
        client.disconnect();
    }

    @Test
    void normalServerCloseDoesNotReconnect() throws Exception {
        FakeTransportFactory transports = new FakeTransportFactory();
        ManagedWebSocketClient client = new WebSocketClientManagerFactory(
                transports, scheduler, List.of()).create(options(), new NoOpListener());

        client.connect();
        transports.latest.closeFromServer(new CloseReason(1000, "Normal closure", false));
        Thread.sleep(30);

        assertThat(transports.created.get()).isEqualTo(1);
        assertThat(client.state()).isEqualTo(ConnectionState.STOPPED);
    }

    private WebSocketClientOptions options() {
        return options(null);
    }

    private WebSocketClientOptions options(Duration lifecycleStageTimeout) {
        return WebSocketClientOptions.builder()
                .connectionId(new ConnectionId("dnse", "market-data"))
                .connectRequest(WebSocketConnectRequest.of(URI.create("wss://example.test/ws"), Duration.ofSeconds(1)))
                .retryPolicy(RetryPolicy.unlimited(Duration.ofMillis(5), Duration.ofMillis(10)))
                .lifecycleStageTimeout(lifecycleStageTimeout)
                .build();
    }

    private static final class NoOpListener implements WebSocketClientListener {
        @Override
        public void onMessage(WebSocketFrame frame) {
        }
    }

    private static final class FakeTransportFactory implements WebSocketTransportFactory {
        private final AtomicInteger created = new AtomicInteger();
        private FakeTransport latest;

        @Override
        public WebSocketTransport create(ConnectionId connectionId) {
            created.incrementAndGet();
            latest = new FakeTransport();
            return latest;
        }
    }

    private static final class FakeTransport implements WebSocketTransport {
        private volatile boolean open;
        private WebSocketTransportListener listener;

        @Override
        public CompletionStage<Void> connect(
                WebSocketConnectRequest request,
                WebSocketTransportListener listener
        ) {
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

        void fail(TransportFailure failure) {
            listener.onFailure(failure);
        }

        void closeFromServer(CloseReason reason) {
            open = false;
            listener.onClosed(reason);
        }
    }
}

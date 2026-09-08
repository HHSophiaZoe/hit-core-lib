package com.hit.websocket.client.transport.reactor;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.connection.WebSocketClient;
import com.hit.websocket.client.connection.WebSocketClientFactory;
import com.hit.websocket.client.connection.WebSocketClientOptions;
import com.hit.websocket.client.connection.HeartbeatOptions;
import com.hit.websocket.client.connection.ReconnectPolicy;
import com.hit.websocket.client.observability.WebSocketObserver;
import com.hit.websocket.client.transport.TransportFailureException;
import com.hit.websocket.client.transport.WebSocketFrame;
import com.hit.websocket.client.transport.WebSocketTransportListener;
import com.hit.websocket.client.transport.WebSocketConnectRequest;
import com.hit.websocket.client.transport.CloseReason;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.DisposableServer;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.time.Duration;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ReactorNettyWebSocketTransportTest {

    @Test
    void nativeHeartbeatWorksWithARealServer() {
        DisposableServer server = HttpServer.create().host("127.0.0.1").port(0)
                .handle((request, response) -> response.sendWebsocket((inbound, outbound) -> inbound.receive().then()))
                .bindNow();
        AtomicInteger received = new AtomicInteger();
        WebSocketObserver observer = new WebSocketObserver() {
            @Override public void onMessageReceived(ConnectionId id, long generation, int bytes) { received.incrementAndGet(); }
        };
        try (WebSocketClientFactory factory = new WebSocketClientFactory(
                new ReactorNettyWebSocketTransportFactory(HttpClient.create()), List.of(observer), Clock.systemDefaultZone())) {
            WebSocketClient client = factory.create(WebSocketClientOptions.builder()
                    .connectionId(new ConnectionId("test", "heartbeat"))
                    .connectRequest(WebSocketConnectRequest.of(URI.create("ws://127.0.0.1:" + server.port()), Duration.ofSeconds(3)))
                    .reconnectPolicy(ReconnectPolicy.disabled())
                    .heartbeat(new HeartbeatOptions(Duration.ofMillis(30), Duration.ofSeconds(1)))
                    .build(), frame -> { });
            client.connect();
            await().atMost(Duration.ofSeconds(5)).until(() -> received.get() >= 3);
            assertThat(client.isHealthy()).isTrue();
        } finally {
            server.disposeNow();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 503})
    void handshakeStatusDeterminesRetryability(int status) {
        DisposableServer server = HttpServer.create().host("127.0.0.1").port(0)
                .handle((request, response) -> response.status(status).send()).bindNow();
        ReactorNettyWebSocketTransport transport = new ReactorNettyWebSocketTransport(HttpClient.create(), new ConnectionId("test", "handshake"));
        try {
            assertThatThrownBy(() -> transport.connect(WebSocketConnectRequest.of(
                            URI.create("ws://127.0.0.1:" + server.port()), Duration.ofSeconds(3)), mock(WebSocketTransportListener.class))
                    .toCompletableFuture().join())
                    .hasCauseInstanceOf(TransportFailureException.class)
                    .cause().satisfies(error -> {
                        TransportFailureException failure = (TransportFailureException) error;
                        assertThat(failure.failure().code()).isEqualTo("HTTP_" + status);
                        assertThat(failure.failure().retryable()).isEqualTo(status != 401);
                    });
        } finally {
            transport.disconnect(CloseReason.normal()).toCompletableFuture().join();
            server.disposeNow();
        }
    }

    @Test
    void disconnectCancelsAnUnfinishedHandshake() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        DisposableServer server = HttpServer.create().host("127.0.0.1").port(0).handle((request, response) -> {
            requested.countDown();
            return Mono.never();
        }).bindNow();
        ReactorNettyWebSocketTransport transport = new ReactorNettyWebSocketTransport(HttpClient.create(), new ConnectionId("test", "cancel"));
        try {
            CompletableFuture<Void> connecting = transport.connect(WebSocketConnectRequest.of(
                    URI.create("ws://127.0.0.1:" + server.port()), Duration.ofSeconds(5)), mock(WebSocketTransportListener.class)).toCompletableFuture();
            assertThat(requested.await(3, TimeUnit.SECONDS)).isTrue();
            transport.disconnect(CloseReason.normal()).toCompletableFuture().get(3, TimeUnit.SECONDS);
            assertThat(connecting).isCompletedExceptionally();
            assertThat(transport.isOpen()).isFalse();
        } finally {
            transport.disconnect(CloseReason.normal());
            server.disposeNow();
        }
    }

    @Test
    void concurrentSendersDeliverAllFramesThroughOneOutboundStream() throws Exception {
        DisposableServer server = HttpServer.create().host("127.0.0.1").port(0)
                .handle((request, response) -> response.sendWebsocket((inbound, outbound) ->
                        outbound.sendString(inbound.receive().asString()))).bindNow();
        ReactorNettyWebSocketTransport transport = new ReactorNettyWebSocketTransport(HttpClient.create(), new ConnectionId("test", "send"));
        Set<String> received = ConcurrentHashMap.newKeySet();
        WebSocketTransportListener listener = mock(WebSocketTransportListener.class);
        doAnswer(invocation -> {
            WebSocketFrame.Text frame = invocation.getArgument(0);
            received.add(frame.payload());
            return null;
        }).when(listener).onFrame(any());
        try (ExecutorService senders = Executors.newFixedThreadPool(8)) {
            transport.connect(WebSocketConnectRequest.of(URI.create("ws://127.0.0.1:" + server.port()), Duration.ofSeconds(3)), listener)
                    .toCompletableFuture().get(3, TimeUnit.SECONDS);
            List<CompletableFuture<Void>> sent = java.util.stream.IntStream.range(0, 200).mapToObj(index ->
                    CompletableFuture.runAsync(() -> transport.send(new WebSocketFrame.Text("frame-" + index))
                            .toCompletableFuture().join(), senders)).toList();
            CompletableFuture.allOf(sent.toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(5)).until(() -> received.size() == 200);
        } finally {
            transport.disconnect(CloseReason.normal()).toCompletableFuture().get(3, TimeUnit.SECONDS);
            server.disposeNow();
        }
    }

    @Test
    void serverCloseCompletesOutboundAndPreservesCloseCode() throws Exception {
        DisposableServer server = HttpServer.create().host("127.0.0.1").port(0)
                .handle((request, response) -> response.sendWebsocket((inbound, outbound) -> outbound.sendClose(1001, "restart")))
                .bindNow();
        ReactorNettyWebSocketTransport transport = new ReactorNettyWebSocketTransport(HttpClient.create(), new ConnectionId("test", "close"));
        AtomicReference<CloseReason> reason = new AtomicReference<>();
        CountDownLatch closed = new CountDownLatch(1);
        WebSocketTransportListener listener = mock(WebSocketTransportListener.class);
        doAnswer(invocation -> {
            reason.set(invocation.getArgument(0));
            closed.countDown();
            return null;
        }).when(listener).onClosed(any());
        try {
            transport.connect(WebSocketConnectRequest.of(URI.create("ws://127.0.0.1:" + server.port()), Duration.ofSeconds(3)), listener)
                    .toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertThat(closed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(reason.get().code()).isEqualTo(1001);
            assertThat(reason.get().initiatedByClient()).isFalse();
            assertThat(transport.isOpen()).isFalse();
        } finally {
            transport.disconnect(CloseReason.normal()).toCompletableFuture().get(3, TimeUnit.SECONDS);
            server.disposeNow();
        }
    }

    @Test
    void inboundFrameIsCopiedWithoutReleasingReactorsBuffer() {
        ReactorNettyWebSocketTransport transport = new ReactorNettyWebSocketTransport(HttpClient.create(), new ConnectionId("test", "buffer"));
        WebSocketTransportListener listener = mock(WebSocketTransportListener.class);
        AtomicReference<WebSocketFrame.Binary> received = new AtomicReference<>();
        doAnswer(invocation -> {
            received.set(invocation.getArgument(0));
            return null;
        }).when(listener).onFrame(any());
        ReflectionTestUtils.setField(transport, "listener", listener);
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            buffer.writeBytes(new byte[]{1, 2, 3});
            NettyDataBufferFactory factory = new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);
            WebSocketMessage message = new WebSocketMessage(WebSocketMessage.Type.BINARY, factory.wrap(buffer));
            ReflectionTestUtils.invokeMethod(transport, "dispatch", message);
            assertThat(buffer.refCnt()).isEqualTo(1);
        } finally {
            if (buffer.refCnt() > 0) buffer.release();
        }
        assertThat(received.get().payload()).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }
}

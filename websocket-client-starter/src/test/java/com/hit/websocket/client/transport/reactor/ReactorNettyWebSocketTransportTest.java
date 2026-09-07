package com.hit.websocket.client.transport.reactor;

import com.hit.websocket.client.connection.ConnectionId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ReactorNettyWebSocketTransportTest {

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

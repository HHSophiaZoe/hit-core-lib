package com.hit.websocket.client.transport.reactor;

import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.transport.FailureCategory;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.TransportFailureException;
import com.hit.websocket.client.transport.WebSocketConnectRequest;
import com.hit.websocket.client.transport.WebSocketFrame;
import com.hit.websocket.client.transport.WebSocketTransport;
import com.hit.websocket.client.transport.WebSocketTransportListener;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Reactor Netty adapter. No Reactor or Netty type escapes through the public transport API. */
final class ReactorNettyWebSocketTransport implements WebSocketTransport {

    private static final int OUTBOUND_CAPACITY = 1024;

    private final HttpClient baseHttpClient;
    private final ConnectionId connectionId;
    private final AtomicReference<WebSocketSession> session = new AtomicReference<>();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private volatile boolean opened;
    private final AtomicBoolean terminated = new AtomicBoolean();
    private final AtomicBoolean clientClosing = new AtomicBoolean();
    private final Sinks.Many<WebSocketFrame> outbound = Sinks.many().unicast()
            .onBackpressureBuffer(new ArrayBlockingQueue<>(OUTBOUND_CAPACITY));

    private volatile WebSocketTransportListener listener;
    private volatile Disposable connectionSubscription;
    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> disconnectFuture = new CompletableFuture<>();
    private volatile CloseStatus closeStatus = CloseStatus.NO_STATUS_CODE;

    ReactorNettyWebSocketTransport(HttpClient baseHttpClient, ConnectionId connectionId) {
        this.baseHttpClient = Objects.requireNonNull(baseHttpClient);
        this.connectionId = Objects.requireNonNull(connectionId);
    }

    @Override
    public CompletionStage<Void> connect(WebSocketConnectRequest request, WebSocketTransportListener listener) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(listener);
        if (clientClosing.get() || !connecting.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Transport is already connecting or connected"));
        }
        this.listener = listener;

        int timeoutMillis = Math.toIntExact(Math.clamp(request.connectTimeout().toMillis(), 1, Integer.MAX_VALUE));
        HttpClient configuredClient = baseHttpClient
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis);
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient(
                configuredClient,
                () -> WebsocketClientSpec.builder()
                        .maxFramePayloadLength(request.maxFramePayloadBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.headers());
        WebSocketHandler handler = new WebSocketHandler() {
            @Override
            public List<String> getSubProtocols() {
                return request.subProtocols();
            }

            @Override
            public Mono<Void> handle(WebSocketSession openedSession) {
                session.set(openedSession);
                if (clientClosing.get()) return openedSession.close();
                opened = true;
                connectFuture.complete(null);

                Mono<Void> receive = openedSession.receive()
                        .doOnNext(ReactorNettyWebSocketTransport.this::dispatch)
                        .doOnComplete(ReactorNettyWebSocketTransport.this::completeOutbound)
                        .then();
                Mono<Void> send = openedSession.send(
                        outbound.asFlux().map(frame -> toSpringFrame(openedSession, frame)));
                return Mono.when(receive, send)
                        .then(openedSession.closeStatus())
                        .doOnNext(status -> closeStatus = status)
                        .then();
            }
        };

        connectionSubscription = client.execute(request.uri(), headers, handler)
                .subscribe(ignored -> { }, this::onTerminalError, this::onTerminalComplete);
        if (clientClosing.get()) connectionSubscription.dispose();
        connectFuture.orTimeout(request.connectTimeout().toNanos(), java.util.concurrent.TimeUnit.NANOSECONDS)
                .whenComplete((ignored, error) -> {
                    if (error != null) disconnect(CloseReason.normal());
                });
        return connectFuture;
    }

    @Override
    public CompletionStage<Void> send(WebSocketFrame frame) {
        Objects.requireNonNull(frame);
        if (!isOpen()) {
            return CompletableFuture.failedFuture(new IllegalStateException("WebSocket transport " + connectionId.value() + " is not open"));
        }
        Sinks.EmitResult result;
        synchronized (outbound) {
            result = outbound.tryEmitNext(frame);
        }
        if (result.isSuccess()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("WebSocket outbound queue rejected frame: " + result));
    }

    @Override
    public CompletionStage<Void> disconnect(CloseReason reason) {
        Objects.requireNonNull(reason);
        if (!clientClosing.compareAndSet(false, true)) return disconnectFuture;
        connectFuture.completeExceptionally(new IllegalStateException("WebSocket connection was cancelled"));
        completeOutbound();
        WebSocketSession current = session.getAndSet(null);
        if (current != null && current.isOpen()) {
            try {
                current.close(CloseStatus.create(reason.code(), reason.reason()))
                        .timeout(Duration.ofSeconds(3))
                        .doFinally(ignored -> disposeConnection())
                        .subscribe(ignored -> { }, disconnectFuture::completeExceptionally,
                                () -> disconnectFuture.complete(null));
            } catch (RuntimeException error) {
                disposeConnection();
                disconnectFuture.completeExceptionally(error);
            }
        } else {
            disposeConnection();
            disconnectFuture.complete(null);
        }
        return disconnectFuture;
    }

    private void disposeConnection() {
        Disposable subscription = connectionSubscription;
        if (subscription != null) subscription.dispose();
    }

    @Override
    public boolean isOpen() {
        WebSocketSession current = session.get();
        return opened && !clientClosing.get() && !terminated.get() && current != null && current.isOpen();
    }

    private void completeOutbound() {
        synchronized (outbound) {
            outbound.tryEmitComplete();
        }
    }

    private void dispatch(WebSocketMessage message) {
        // Copy synchronously: Reactor owns and releases the pooled inbound buffer.
        DataBuffer payload = message.getPayload();
        try {
            WebSocketFrame frame = switch (message.getType()) {
                case TEXT -> new WebSocketFrame.Text(message.getPayloadAsText());
                case BINARY -> new WebSocketFrame.Binary(copy(payload));
                case PING -> new WebSocketFrame.Ping(copy(payload));
                case PONG -> new WebSocketFrame.Pong(copy(payload));
            };
            listener.onFrame(frame);
        } catch (RuntimeException error) {
            throw new TransportFailureException(new TransportFailure(
                    FailureCategory.PROTOCOL, "FRAME_PROCESSING_FAILED", safeMessage(error), true), error);
        }
    }

    private WebSocketMessage toSpringFrame(WebSocketSession currentSession, WebSocketFrame frame) {
        return switch (frame) {
            case WebSocketFrame.Text text -> currentSession.textMessage(text.payload());
            case WebSocketFrame.Binary binary -> currentSession.binaryMessage(factory -> factory.wrap(binary.payload()));
            case WebSocketFrame.Ping ping -> currentSession.pingMessage(factory -> factory.wrap(ping.payload()));
            case WebSocketFrame.Pong pong -> currentSession.pongMessage(factory -> factory.wrap(pong.payload()));
        };
    }

    private void onTerminalError(Throwable error) {
        if (!terminated.compareAndSet(false, true)) {
            return;
        }
        session.set(null);
        if (!opened) {
            TransportFailure failure = classify(error);
            connectFuture.completeExceptionally(new TransportFailureException(failure, error));
            return;
        }
        listener.onFailure(classify(error));
    }

    private void onTerminalComplete() {
        if (!terminated.compareAndSet(false, true)) {
            return;
        }
        session.set(null);
        if (!opened) {
            connectFuture.completeExceptionally(new IllegalStateException("WebSocket completed before opening"));
            return;
        }
        listener.onClosed(new CloseReason(closeStatus.getCode(), closeStatus.getReason(), clientClosing.get()));
    }

    private static byte[] copy(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        return bytes;
    }

    private static TransportFailure classify(Throwable error) {
        Objects.requireNonNull(error, "error");
        // Keep typed failures anywhere in the chain; inspecting only the root loses HTTP/TLS context.
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof TransportFailureException failure) return failure.failure();
            if (cause instanceof SSLException) return classified(FailureCategory.TLS, cause, false);
            if (cause instanceof WebSocketClientHandshakeException handshake && handshake.response() != null) {
                int status = handshake.response().status().code();
                return new TransportFailure(FailureCategory.HANDSHAKE, "HTTP_" + status,
                        "WebSocket handshake rejected (HTTP " + status + ")", status == 408 || status == 429 || status >= 500);
            }
            if (cause instanceof WebSocketHandshakeException) return classified(FailureCategory.HANDSHAKE, cause, false);
            if (cause instanceof UnknownHostException) return classified(FailureCategory.DNS, cause, true);
            if (cause instanceof TimeoutException) return classified(FailureCategory.CONNECT_TIMEOUT, cause, true);
            if (cause instanceof ConnectException) return classified(FailureCategory.NETWORK, cause, true);
            if (cause.getCause() == cause) break;
        }
        return classified(FailureCategory.UNKNOWN, error, true);
    }

    private static TransportFailure classified(FailureCategory category, Throwable error, boolean retryable) {
        // Exception messages can contain request URLs/tokens. Keep transport diagnostics safe for metrics/API.
        return new TransportFailure(category, error.getClass().getSimpleName(), "WebSocket transport failure: " + category, retryable);
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}

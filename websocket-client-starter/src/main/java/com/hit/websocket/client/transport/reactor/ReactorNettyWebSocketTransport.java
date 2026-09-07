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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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
    private final AtomicBoolean opened = new AtomicBoolean();
    private final AtomicBoolean terminated = new AtomicBoolean();
    private final AtomicBoolean clientClosing = new AtomicBoolean();
    private final Sinks.Many<WebSocketFrame> outbound = Sinks.many().unicast()
            .onBackpressureBuffer(new ArrayBlockingQueue<>(OUTBOUND_CAPACITY));

    private volatile WebSocketTransportListener listener;
    private volatile Disposable connectionSubscription;
    private volatile CompletableFuture<Void> connectFuture;

    ReactorNettyWebSocketTransport(HttpClient baseHttpClient, ConnectionId connectionId) {
        this.baseHttpClient = Objects.requireNonNull(baseHttpClient);
        this.connectionId = Objects.requireNonNull(connectionId);
    }

    @Override
    public CompletionStage<Void> connect(WebSocketConnectRequest request, WebSocketTransportListener listener) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(listener);
        if (!connecting.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Transport is already connecting or connected"));
        }
        this.listener = listener;
        this.connectFuture = new CompletableFuture<>();

        int timeoutMillis = Math.toIntExact(Math.min(
                request.connectTimeout().toMillis(), Integer.MAX_VALUE));
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
                opened.set(true);
                connectFuture.complete(null);

                Mono<Void> receive = openedSession.receive()
                        .doOnNext(message -> dispatch(openedSession, message))
                        .then();
                Mono<Void> send = openedSession.send(
                        outbound.asFlux().map(frame -> toSpringFrame(openedSession, frame)));
                return Mono.when(receive, send);
            }
        };

        connectionSubscription = client.execute(request.uri(), headers, handler)
                .subscribe(ignored -> { }, this::onTerminalError, this::onTerminalComplete);
        return connectFuture.orTimeout(
                request.connectTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletionStage<Void> send(WebSocketFrame frame) {
        Objects.requireNonNull(frame);
        if (!isOpen()) {
            return CompletableFuture.failedFuture(new IllegalStateException("WebSocket transport " + connectionId.value() + " is not open"));
        }
        Sinks.EmitResult result = outbound.tryEmitNext(frame);
        if (result.isSuccess()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("WebSocket outbound queue rejected frame: " + result));
    }

    @Override
    public CompletionStage<Void> disconnect(CloseReason reason) {
        clientClosing.set(true);
        outbound.tryEmitComplete();
        WebSocketSession current = session.getAndSet(null);
        if (current != null && current.isOpen()) {
            return current.close(CloseStatus.create(reason.code(), reason.reason())).toFuture();
        }
        Disposable subscription = connectionSubscription;
        if (subscription != null) {
            subscription.dispose();
        }
        CompletableFuture<Void> pendingConnect = connectFuture;
        if (pendingConnect != null && !pendingConnect.isDone()) {
            pendingConnect.completeExceptionally(new IllegalStateException("WebSocket connection was cancelled"));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOpen() {
        WebSocketSession current = session.get();
        return opened.get() && !terminated.get() && current != null && current.isOpen();
    }

    private void dispatch(WebSocketSession currentSession, WebSocketMessage message) {
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
            listener.onFailure(new TransportFailure(FailureCategory.PROTOCOL, "FRAME_PROCESSING_FAILED", safeMessage(error), true));
            currentSession.close(CloseStatus.PROTOCOL_ERROR).subscribe();
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
        CompletableFuture<Void> pendingConnect = connectFuture;
        if (!opened.get() && pendingConnect != null) {
            TransportFailure failure = classify(error);
            pendingConnect.completeExceptionally(new TransportFailureException(failure, error));
            return;
        }
        listener.onFailure(classify(error));
    }

    private void onTerminalComplete() {
        if (!terminated.compareAndSet(false, true)) {
            return;
        }
        WebSocketSession closedSession = session.getAndSet(null);
        if (!opened.get()) {
            CompletableFuture<Void> pendingConnect = connectFuture;
            if (pendingConnect != null) {
                pendingConnect.completeExceptionally(
                        new IllegalStateException("WebSocket completed before opening"));
            }
            return;
        }
        if (closedSession == null) {
            listener.onClosed(new CloseReason(1000, "Connection closed", clientClosing.get()));
            return;
        }
        closedSession.closeStatus()
                .defaultIfEmpty(CloseStatus.NO_STATUS_CODE)
                .subscribe(status -> listener.onClosed(new CloseReason(
                        status.getCode(), status.getReason(), clientClosing.get())));
    }

    private static byte[] copy(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        return bytes;
    }

    private static TransportFailure classify(Throwable error) {
        Throwable cause = unwrap(error);
        FailureCategory category;
        if (cause instanceof UnknownHostException) {
            category = FailureCategory.DNS;
        } else if (cause instanceof TimeoutException) {
            category = FailureCategory.CONNECT_TIMEOUT;
        } else if (cause instanceof ConnectException) {
            category = FailureCategory.NETWORK;
        } else if (cause.getClass().getSimpleName().toLowerCase().contains("ssl")) {
            category = FailureCategory.TLS;
        } else if (isHandshakeFailure(cause)) {
            category = FailureCategory.HANDSHAKE;
        } else {
            category = FailureCategory.UNKNOWN;
        }
        boolean retryable = category != FailureCategory.HANDSHAKE || isRetryableHandshake(cause);
        return new TransportFailure(category, cause.getClass().getSimpleName(), safeMessage(cause), retryable);
    }

    private static boolean isHandshakeFailure(Throwable error) {
        String type = error.getClass().getSimpleName().toLowerCase();
        String message = safeMessage(error).toLowerCase();
        return type.contains("handshake") || message.contains("handshake") || message.matches(".*\\b(401|403|404|408|429|5\\d\\d)\\b.*");
    }

    private static boolean isRetryableHandshake(Throwable error) {
        String message = safeMessage(error);
        return message.matches(".*\\b(408|429|5\\d\\d)\\b.*");
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}

package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.FailureCategory;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketFrame;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface ManagedWebSocketClient extends AutoCloseable {

    void connect();

    void disconnect();

    CompletionStage<Void> send(WebSocketFrame frame);

    boolean isConnected();

    boolean isHealthy();

    ConnectionState state();

    void setHeartbeatFrameSupplier(Supplier<WebSocketFrame> supplier);

    void markPongReceived();

    void markAuthenticating();

    void markResubscribing();

    void markReady();

    void reportError(FailureCategory category, String code, String message);

    /** Report a terminal protocol/application failure and apply the configured retry policy. */
    void fail(TransportFailure failure);

    @Override
    default void close() {
        disconnect();
    }
}

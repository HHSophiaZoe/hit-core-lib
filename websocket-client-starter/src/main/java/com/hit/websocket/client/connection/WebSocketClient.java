package com.hit.websocket.client.connection;

import com.hit.websocket.client.transport.FailureCategory;
import com.hit.websocket.client.transport.TransportFailure;
import com.hit.websocket.client.transport.WebSocketFrame;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface WebSocketClient extends AutoCloseable {

    void connect();

    void disconnect();

    CompletionStage<Void> send(WebSocketFrame frame);

    boolean isConnected();

    boolean isHealthy();

    ConnectionState state();

    /** Overrides native Ping; returning null skips this tick (for example while authenticating). */
    void setHeartbeatFrameSupplier(Supplier<WebSocketFrame> supplier);

    void markPongReceived();

    void reportError(FailureCategory category, String code, String message);

    /** Report a terminal protocol/application failure and apply the configured retry policy. */
    void fail(TransportFailure failure);

    @Override
    void close();
}

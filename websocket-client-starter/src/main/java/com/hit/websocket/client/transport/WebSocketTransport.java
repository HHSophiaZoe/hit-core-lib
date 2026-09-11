package com.hit.websocket.client.transport;

import java.util.concurrent.CompletionStage;

/**
 * A single WebSocket transport connection. Implementations must serialize outbound frames.
 * The send stage may complete once a frame is accepted by the outbound pipeline.
 */
public interface WebSocketTransport {

    CompletionStage<Void> connect(WebSocketConnectRequest request, WebSocketTransportListener listener);

    /**
     * Accept a frame into the transport's bounded outbound path without waiting for network I/O completion.
     * Implementations must fail the returned stage immediately when the frame cannot be accepted.
     */
    CompletionStage<Void> send(WebSocketFrame frame);

    CompletionStage<Void> disconnect(CloseReason reason);

    boolean isOpen();

}

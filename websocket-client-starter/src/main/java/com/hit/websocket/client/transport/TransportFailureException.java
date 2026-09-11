package com.hit.websocket.client.transport;

public class TransportFailureException extends RuntimeException {

    private final TransportFailure failure;

    public TransportFailureException(TransportFailure failure, Throwable cause) {
        super(failure.message(), cause);
        this.failure = failure;
    }

    public TransportFailure failure() {
        return failure;
    }
}

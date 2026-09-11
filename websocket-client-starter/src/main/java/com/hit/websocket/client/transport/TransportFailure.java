package com.hit.websocket.client.transport;

public record TransportFailure(
        FailureCategory category,
        String code,
        String message,
        boolean retryable
) {
    public TransportFailure {
        category = category == null ? FailureCategory.UNKNOWN : category;
        code = code == null ? category.name() : code;
        message = message == null ? "" : message;
    }

    public static TransportFailure unknown(Throwable error) {
        String message = error == null ? "Unknown transport error" : error.getMessage();
        String code = error == null ? FailureCategory.UNKNOWN.name() : error.getClass().getSimpleName();
        return new TransportFailure(FailureCategory.UNKNOWN, code, message, true);
    }
}

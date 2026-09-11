package com.hit.websocket.client.transport;

public record CloseReason(int code, String reason, boolean initiatedByClient) {

    public CloseReason {
        reason = reason == null ? "" : reason;
    }

    public static CloseReason normal() {
        return new CloseReason(1000, "Normal closure", true);
    }

    public static CloseReason unavailable(String reason) {
        return new CloseReason(1006, reason, false);
    }
}

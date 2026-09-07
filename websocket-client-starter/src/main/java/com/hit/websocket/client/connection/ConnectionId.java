package com.hit.websocket.client.connection;

import java.util.Objects;

/** Stable logical identity. It must not be a transport session id. */
public record ConnectionId(String provider, String name) {

    public ConnectionId {
        provider = requireText(provider, "provider");
        name = requireText(name, "name");
    }

    public String value() {
        return provider + ":" + name;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return normalized;
    }
}

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

    public static ConnectionId parse(String value) {
        Objects.requireNonNull(value, "connectionId cannot be null");
        int separator = value.indexOf(':');
        if (separator < 1 || separator == value.length() - 1) {
            throw new IllegalArgumentException("connectionId must use source:name format");
        }
        return new ConnectionId(value.substring(0, separator), value.substring(separator + 1));
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

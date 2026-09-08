package com.hit.websocket.client.transport;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record WebSocketConnectRequest(
        URI uri,
        Map<String, List<String>> headers,
        List<String> subProtocols,
        Duration connectTimeout,
        int maxFramePayloadBytes
) {
    public WebSocketConnectRequest {
        Objects.requireNonNull(uri, "uri cannot be null");
        if (!"ws".equalsIgnoreCase(uri.getScheme()) && !"wss".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("uri scheme must be ws or wss");
        }
        headers = immutableHeaders(headers);
        subProtocols = subProtocols == null ? List.of() : List.copyOf(subProtocols);
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        connectTimeout.toNanos();
        maxFramePayloadBytes = maxFramePayloadBytes == 0 ? 1024 * 1024 : maxFramePayloadBytes;
        if (maxFramePayloadBytes <= 0) {
            throw new IllegalArgumentException("maxFramePayloadBytes must be positive");
        }
    }

    public static WebSocketConnectRequest of(URI uri, Duration connectTimeout) {
        return builder().uri(uri).connectTimeout(connectTimeout).build();
    }

    private static Map<String, List<String>> immutableHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        return headers.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                entry -> Objects.requireNonNull(entry.getKey(), "header name cannot be null"),
                entry -> List.copyOf(entry.getValue())
        ));
    }
}

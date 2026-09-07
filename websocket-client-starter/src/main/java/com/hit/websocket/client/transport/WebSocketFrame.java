package com.hit.websocket.client.transport;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Transport-neutral WebSocket frame. */
public sealed interface WebSocketFrame {

    /** Payload size without cloning binary/control frame storage for metrics. */
    default int payloadSize() {
        return switch (this) {
            case Text text -> text.bytes().length;
            case Binary binary -> binary.payload.length;
            case Ping ping -> ping.payload.length;
            case Pong pong -> pong.payload.length;
        };
    }

    record Text(String payload) implements WebSocketFrame {
        public Text {
            Objects.requireNonNull(payload, "payload cannot be null");
        }

        public byte[] bytes() {
            return payload.getBytes(StandardCharsets.UTF_8);
        }
    }

    record Binary(byte[] payload) implements WebSocketFrame {
        public Binary {
            Objects.requireNonNull(payload, "payload cannot be null");
            payload = payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    record Ping(byte[] payload) implements WebSocketFrame {
        public Ping {
            payload = payload == null ? new byte[0] : payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    record Pong(byte[] payload) implements WebSocketFrame {
        public Pong {
            payload = payload == null ? new byte[0] : payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }
}

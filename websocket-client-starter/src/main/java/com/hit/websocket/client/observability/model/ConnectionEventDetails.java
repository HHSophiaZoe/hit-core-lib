package com.hit.websocket.client.observability.model;

import com.hit.websocket.client.transport.CloseReason;
import com.hit.websocket.client.transport.FailureCategory;

import java.time.Duration;

/** Typed lifecycle data. Applications may implement this interface for diagnostic-only custom events. */
public interface ConnectionEventDetails {

    record None() implements ConnectionEventDetails {
        public static final None INSTANCE = new None();
    }

    record ConnectAttempt(int attempt) implements ConnectionEventDetails {
    }

    record RetryScheduled(int attempt, int maxAttempts, Duration delay) implements ConnectionEventDetails {
    }

    record Failure(
            FailureCategory category,
            String code,
            String message,
            boolean retryable,
            boolean terminal
    ) implements ConnectionEventDetails {
    }

    record TransportClosed(CloseReason reason) implements ConnectionEventDetails {
    }

    record LifecycleFailed(Reason reason, Integer attempt) implements ConnectionEventDetails {
        public enum Reason {
            NON_RETRYABLE_FAILURE,
            RETRY_EXHAUSTED
        }
    }

    record CloseFailed(String message) implements ConnectionEventDetails {
    }
}

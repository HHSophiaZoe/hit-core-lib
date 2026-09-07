package com.hit.websocket.client.connection;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public record RetryPolicy(
        int maxAttempts,
        Duration initialDelay,
        Duration maxDelay,
        double jitter
) {
    public RetryPolicy {
        if (maxAttempts < -1) {
            throw new IllegalArgumentException("maxAttempts must be -1, 0, or positive");
        }
        if (initialDelay == null || initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay cannot be null or negative");
        }
        if (maxDelay == null || maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= initialDelay");
        }
        if (jitter < 0 || jitter > 1) {
            throw new IllegalArgumentException("jitter must be between 0 and 1");
        }
    }

    public static RetryPolicy unlimited(Duration initialDelay, Duration maxDelay) {
        return new RetryPolicy(-1, initialDelay, maxDelay, 0.2);
    }

    public static RetryPolicy disabled() {
        return new RetryPolicy(0, Duration.ZERO, Duration.ZERO, 0);
    }

    public boolean allows(int nextAttempt) {
        return maxAttempts < 0 || nextAttempt <= maxAttempts;
    }

    public Duration delayFor(int attempt) {
        if (attempt <= 0 || initialDelay.isZero()) {
            return Duration.ZERO;
        }
        int shift = Math.min(attempt - 1, 30);
        long initialMillis = initialDelay.toMillis();
        long maxMillis = maxDelay.toMillis();
        long exponential;
        try {
            exponential = Math.multiplyExact(initialMillis, 1L << shift);
        } catch (ArithmeticException ignored) {
            exponential = maxMillis;
        }
        long capped = Math.min(exponential, maxMillis);
        if (jitter == 0 || capped == 0) {
            return Duration.ofMillis(capped);
        }
        double factor = ThreadLocalRandom.current().nextDouble(1 - jitter, 1 + jitter);
        return Duration.ofMillis(Math.clamp(Math.round(capped * factor), 0, maxMillis));
    }
}

package com.hit.common.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class ThreadUtils {

    /**
     * Sleeps; when interrupted, keeps the interrupt flag and throws {@link IllegalStateException}.
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sleeping", e);
        }
    }

}

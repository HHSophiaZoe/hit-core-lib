package com.hit.spring.utils;

import lombok.experimental.UtilityClass;

import java.time.Duration;

@UtilityClass
public class ThreadUtils {

    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

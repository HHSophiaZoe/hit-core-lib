package com.hit.spring.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class TraceUtils {

    public static String generateTraceId() {
        UUID uuid = UUID.randomUUID();
        return String.format("%016x%016x", uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

}

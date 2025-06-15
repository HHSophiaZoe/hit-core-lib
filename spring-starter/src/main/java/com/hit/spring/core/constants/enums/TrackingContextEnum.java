package com.hit.spring.core.constants.enums;

import com.hit.spring.utils.TraceUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

import static com.hit.spring.core.constants.CommonConstant.CommonSymbol.SHIFT_DASH;

@Getter
@AllArgsConstructor
public enum TrackingContextEnum {

    CORRELATION_ID("correlation_id"),
    THREAD_ID("thread_id");

    private final String key;

    public static String genCorrelationId(String appName) {
        return genCorrelationId(null, appName);
    }

    public static String genCorrelationId(byte[] correlationIdByte, String appName) {
        if (correlationIdByte != null) {
            return new String(correlationIdByte);
        }
        return (appName + SHIFT_DASH + TraceUtils.generateTraceId()).trim();
    }

    public static String genThreadId() {
        return TraceUtils.generateTraceId();
    }
}

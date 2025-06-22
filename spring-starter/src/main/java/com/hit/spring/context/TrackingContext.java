package com.hit.spring.context;

import com.hit.spring.util.TraceUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.ThreadContext;

import java.util.Objects;

import static com.hit.spring.core.constant.CommonConstant.CommonSymbol.SHIFT_DASH;

@Getter
@AllArgsConstructor
public class TrackingContext {

    public static final String CORRELATION_ID = "correlation_id";
    public static final String THREAD_ID = "thread_id";

    public static String genCorrelationId(String appName) {
        return genCorrelationId(null, appName);
    }

    public static String genCorrelationId(byte[] correlationIdByte, String appName) {
        if (correlationIdByte != null) {
            return new String(correlationIdByte);
        }
        return (appName + SHIFT_DASH + TraceUtils.generateTraceId()).trim();
    }

    public static void setCorrelationId(String correlationId) {
        ThreadContext.put(TrackingContext.CORRELATION_ID, Objects.requireNonNullElseGet(correlationId, TraceUtils::generateTraceId));
    }

    public static String getCorrelationId() {
        return ThreadContext.get(TrackingContext.CORRELATION_ID);
    }

    public static void setThreadId() {
        ThreadContext.put(THREAD_ID, TraceUtils.generateTraceId());
    }

    public static void clearTrackingContext() {
        ThreadContext.clearAll();
    }
}

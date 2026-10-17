package com.hit.spring.tracing;

import com.hit.common.util.TraceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * Trace id of the current thread, kept in the SLF4J MDC under {@link #TRACE_ID} so every log line carries it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrackingContext {

    public static final String TRACE_ID = "trace_id";

    public static String genTraceId() {
        return TraceUtils.generateTraceId();
    }

    public static String genTraceId(byte[] traceIdBytes) {
        if (traceIdBytes != null && traceIdBytes.length > 0) {
            return new String(traceIdBytes);
        }
        return TraceUtils.generateTraceId();
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId != null ? traceId : TraceUtils.generateTraceId());
    }

    /**
     * Trace id of the current thread, or a new id when the thread has none (the new id is not stored).
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        return traceId != null ? traceId : TraceUtils.generateTraceId();
    }

    /**
     * Trace id of the current thread, or null when none is set.
     */
    public static String currentTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Sets the trace id, or removes it when null; used to put back a previous value.
     */
    public static void restoreTraceId(String traceId) {
        if (traceId == null) {
            MDC.remove(TRACE_ID);
        } else {
            MDC.put(TRACE_ID, traceId);
        }
    }

    public static void clearContext() {
        MDC.remove(TRACE_ID);
    }
}

package com.hit.spring.context;

import com.hit.common.util.TraceUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.logging.log4j.ThreadContext;

import java.util.Objects;

@Getter
@AllArgsConstructor
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
        ThreadContext.put(TrackingContext.TRACE_ID, Objects.requireNonNullElseGet(traceId, TraceUtils::generateTraceId));
    }

    public static String getTraceId() {
        return Objects.requireNonNullElseGet(ThreadContext.get(TrackingContext.TRACE_ID), TraceUtils::generateTraceId);
    }

    public static void clearContext() {
        ThreadContext.clearAll();
    }
}

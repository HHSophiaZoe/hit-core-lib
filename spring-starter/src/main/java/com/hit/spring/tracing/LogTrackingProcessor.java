package com.hit.spring.tracing;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Gives a {@code @LogTracking} method a trace id when the calling thread has none (scheduled jobs, listeners).
 */
@Slf4j
@Aspect
public class LogTrackingProcessor {

    @Around("@annotation(com.hit.spring.tracing.LogTracking)")
    public Object aroundTrackedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (TrackingContext.currentTraceId() != null) {
            return joinPoint.proceed();
        }
        TrackingContext.setTraceId(TrackingContext.genTraceId());
        log.debug("TraceId initialized: {}, method={}", TrackingContext.currentTraceId(), joinPoint.getSignature());
        try {
            return joinPoint.proceed();
        } finally {
            TrackingContext.clearContext();
        }
    }
}

package com.hit.spring.annotation.processor;

import com.hit.spring.context.TrackingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Aspect
@Configuration
@RequiredArgsConstructor
public class LogTrackingProcessor {

    @Around("@annotation(com.hit.spring.annotation.LogTracking)")
    public Object aroundTrackedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            TrackingContext.setTraceId(TrackingContext.getTraceId());
            log.debug("TraceId initialized: {}, method={}", TrackingContext.getTraceId(), joinPoint.getSignature());
            return joinPoint.proceed();
        } finally {
            TrackingContext.clearContext();
            log.debug("TraceId cleared method={}", joinPoint.getSignature());
        }
    }

}

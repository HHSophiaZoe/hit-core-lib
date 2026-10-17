package com.hit.spring.web.filter;

import com.hit.spring.tracing.TrackingContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Puts the caller's {@code trace_id} header (or a new id) into the logging context and echoes it in the response.
 */
@Slf4j
public class LogCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String requestTraceId = request.getHeader(TrackingContext.TRACE_ID);
        String traceId = StringUtils.isEmpty(requestTraceId) ? TrackingContext.genTraceId() : requestTraceId;
        TrackingContext.setTraceId(traceId);
        response.setHeader(TrackingContext.TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("{}: {} ms ", request.getRequestURI(), (System.nanoTime() - startNanos) / 1_000_000);
            TrackingContext.clearContext();
        }
    }
}

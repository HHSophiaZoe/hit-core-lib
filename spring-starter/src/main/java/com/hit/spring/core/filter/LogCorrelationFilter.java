package com.hit.spring.core.filter;

import com.hit.spring.config.properties.ApplicationProperties;
import com.hit.spring.context.TrackingContext;
import com.hit.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Order(1)
@Configuration
@RequiredArgsConstructor
public class LogCorrelationFilter extends OncePerRequestFilter {

    private final ApplicationProperties appProperties;

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        long time = System.currentTimeMillis();
        String traceId = this.generateTraceIdIfNotExists(request.getHeader(TrackingContext.TRACE_ID));
        response.setHeader(TrackingContext.TRACE_ID, traceId);
        filterChain.doFilter(request, response);
        log.info("{}: {} ms ", request.getRequestURI(), System.currentTimeMillis() - time);
        TrackingContext.clearContext();
    }


    private String generateTraceIdIfNotExists(String xtraceId) {
        String traceId = StringUtils.isEmpty(xtraceId) ? TrackingContext.genTraceId() : xtraceId;
        TrackingContext.setTraceId(traceId);
        return traceId;
    }
}

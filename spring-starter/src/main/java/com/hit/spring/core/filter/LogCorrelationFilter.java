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
        String correlationId = this.generateCorrelationIdIfNotExists(request.getHeader(TrackingContext.CORRELATION_ID));
        response.setHeader(TrackingContext.CORRELATION_ID, correlationId);
        filterChain.doFilter(request, response);
        log.info("{}: {} ms ", request.getRequestURI(), System.currentTimeMillis() - time);
        TrackingContext.clearContext();
    }


    private String generateCorrelationIdIfNotExists(String xCorrelationId) {
        String correlationId = StringUtils.isEmpty(xCorrelationId)
                ? TrackingContext.genCorrelationId(this.appProperties.getName())
                : xCorrelationId;
        TrackingContext.setCorrelationId(correlationId);
        return correlationId;
    }
}

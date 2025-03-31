package com.hit.spring.core.filter;

import com.hit.spring.config.properties.ApplicationProperties;
import com.hit.spring.core.constants.enums.TrackingContextEnum;
import com.hit.spring.utils.DataUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
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
        this.generateCorrelationIdIfNotExists(request.getHeader(TrackingContextEnum.CORRELATION_ID.getKey()));
        response.setHeader(TrackingContextEnum.CORRELATION_ID.getKey(), ThreadContext.get(TrackingContextEnum.CORRELATION_ID.getKey()));
        filterChain.doFilter(request, response);
        log.info("{}: {} ms ", request.getRequestURI(), System.currentTimeMillis() - time);
        ThreadContext.clearAll();
    }


    private void generateCorrelationIdIfNotExists(String xCorrelationId) {
        String correlationId = org.apache.commons.lang3.StringUtils.isEmpty(xCorrelationId)
                ? DataUtils.genCorrelationId(this.appProperties.getApplicationShortName()) : xCorrelationId;
        ThreadContext.put(TrackingContextEnum.CORRELATION_ID.getKey(), correlationId);
    }
}

package com.hit.spring.core.filter;

import com.hit.spring.config.properties.SecurityProperties;
import com.hit.spring.core.wrapper.CachedBodyRequestWrapper;
import com.hit.spring.util.IPAddressUtils;
import com.hit.spring.util.LoggingUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.time.LocalDateTime;

@Slf4j
@Order(2)
@Component
@ConditionalOnProperty(value = {"app.enable-log-request-http"}, havingValue = "true")
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ActionLogRequest actionLogRequest;

    private final SecurityProperties securityProperties;

    public RequestLoggingFilter(@Autowired(required = false) ActionLogRequest actionLogRequest, SecurityProperties securityProperties) {
        this.actionLogRequest = actionLogRequest;
        this.securityProperties = securityProperties;
    }

    @FunctionalInterface
    public interface ActionLogRequest {
        void process(RequestLogData requestData, ResponseLogData responseData);
    }

    public record RequestLogData(String ip, String url, String header, String body, LocalDateTime requestTime) {
    }

    public record ResponseLogData(String url, String header, Integer status, String body, LocalDateTime responseTime) {
    }

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        if (securityProperties.getApiSkipLogRequest().stream().anyMatch(request.getRequestURI()::contains)) {
            filterChain.doFilter(request, response);
            return;
        }
        LocalDateTime requestTime = LocalDateTime.now();
        try {
            CachedBodyRequestWrapper requestWrapper = new CachedBodyRequestWrapper(request);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

            String url = LoggingUtils.getFullURL(requestWrapper);
            RequestLogData requestLogData = new RequestLogData(
                    IPAddressUtils.getClientIpAddress(requestWrapper),
                    url,
                    LoggingUtils.getHeaders(requestWrapper),
                    LoggingUtils.getRequestBody(requestWrapper),
                    requestTime
            );
            StringBuilder requestStr = new StringBuilder();
            requestStr.append("\n======> HTTP Request: ");
            requestStr.append("\nRequest to   : ").append(requestLogData.url());
            requestStr.append("\nMethod       : ").append(requestWrapper.getMethod());
            requestStr.append("\nHeader       : ").append(requestLogData.header());
            requestStr.append("\nContent-Type : ").append(requestWrapper.getContentType());
            requestStr.append("\nBody         : ").append(requestLogData.body());
            requestStr.append(" \n");
            log.info(requestStr.toString());

            filterChain.doFilter(requestWrapper, responseWrapper);

            ResponseLogData responseLogData = new ResponseLogData(
                    url,
                    LoggingUtils.getHeaders(responseWrapper),
                    responseWrapper.getStatus(),
                    LoggingUtils.getResponseBody(responseWrapper),
                    LocalDateTime.now()
            );
            StringBuilder responseStr = new StringBuilder();
            responseStr.append("\n======> HTTP Response: ");
            responseStr.append("\nResponse to : ").append(requestLogData.url());
            responseStr.append("\nHeader      : ").append(responseLogData.header());
            responseStr.append("\nStatus      : ").append(responseLogData.status());
            responseStr.append("\nBody        : ").append(responseLogData.body());
            responseStr.append(" \n");
            log.info(responseStr.toString());

            if (actionLogRequest != null) {
                actionLogRequest.process(requestLogData, responseLogData);
            }
            responseWrapper.copyBodyToResponse();
        } catch (Exception ex) {
            log.error("Request logging error: {}", ex.getMessage(), ex);
        }
    }

}
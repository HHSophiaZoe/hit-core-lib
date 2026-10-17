package com.hit.spring.web.filter;

import com.hit.spring.security.SecurityProperties;
import com.hit.spring.web.IPAddressUtils;
import com.hit.spring.web.LoggingUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Instant;

/**
 * Logs every request and response with headers and masked body; an optional {@link ActionLogRequest} receives both.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ActionLogRequest actionLogRequest;

    private final SecurityProperties securityProperties;

    public RequestLoggingFilter(ActionLogRequest actionLogRequest, SecurityProperties securityProperties) {
        this.actionLogRequest = actionLogRequest;
        this.securityProperties = securityProperties;
    }

    @FunctionalInterface
    public interface ActionLogRequest {
        void process(RequestLogData requestData, ResponseLogData responseData);
    }

    public record RequestLogData(String ip, String url, String header, String body, Instant requestTime) {
    }

    public record ResponseLogData(String url, String header, Integer status, String body, Instant responseTime) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return securityProperties.isSkipLogPath(RequestPaths.pathWithinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyRequestWrapper requestWrapper = new CachedBodyRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        RequestLogData requestLogData = new RequestLogData(
                IPAddressUtils.getClientIpAddress(requestWrapper),
                LoggingUtils.getFullURL(requestWrapper),
                LoggingUtils.getHeaders(requestWrapper),
                LoggingUtils.getRequestBody(requestWrapper, securityProperties.getSensitiveFieldLogRequest()),
                Instant.now()
        );
        log.info("""

                ======> HTTP Request:\s
                Request to   : {}
                Method       : {}
                Header       : {}
                Content-Type : {}
                Body         : {}\s
                """, requestLogData.url(), requestWrapper.getMethod(), requestLogData.header(),
                requestWrapper.getContentType(), requestLogData.body());
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            this.logResponse(requestLogData, responseWrapper);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logResponse(RequestLogData requestLogData, ContentCachingResponseWrapper responseWrapper) {
        try {
            ResponseLogData responseLogData = new ResponseLogData(
                    requestLogData.url(),
                    LoggingUtils.getHeaders(responseWrapper),
                    responseWrapper.getStatus(),
                    LoggingUtils.getResponseBody(responseWrapper),
                    Instant.now()
            );
            log.info("""

                    ======> HTTP Response:\s
                    Response to : {}
                    Header      : {}
                    Status      : {}
                    Body        : {}\s
                    """, responseLogData.url(), responseLogData.header(), responseLogData.status(),
                    responseLogData.body());
            if (actionLogRequest != null) {
                actionLogRequest.process(requestLogData, responseLogData);
            }
        } catch (Exception e) {
            log.error("Request logging error: {}", e.getMessage(), e);
        }
    }
}

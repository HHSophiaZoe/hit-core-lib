package com.hit.spring.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Order(2)
@Component
@ConditionalOnProperty(value = {"app.enable-log-request-http"}, havingValue = "true")
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final Map<String, String> replaceCharsError = new HashMap<>();

    public RequestLoggingFilter() {
        this.replaceCharsError.put("\u0000", "");
    }

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        if (request.getRequestURI().contains("/swagger-ui") || request.getRequestURI().contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        StringBuilder str = new StringBuilder();
        try {
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(requestWrapper, response);
            str.append("\n======> HTTP Request: ");
            str.append("\nRequest to : ").append(this.getFullURL(requestWrapper));
            str.append("\nMethod     : ").append(requestWrapper.getMethod());
            str.append("\nHeader     : ").append(this.getHeaders(requestWrapper));
            str.append("\nBody       : ").append(this.getBody(requestWrapper));
            str.append(" \n");
            log.info(str.toString());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();
        return queryString == null ? requestURL.toString() : requestURL.append('?').append(queryString).toString();
    }

    public Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> headersName = request.getHeaderNames();
        while (headersName.hasMoreElements()) {
            String name = headersName.nextElement();
            map.put(name, request.getHeader(name));
        }
        return map;
    }

    private String getBody(ContentCachingRequestWrapper requestWrapper) throws UnsupportedEncodingException {
        byte[] content = requestWrapper.getContentAsByteArray();
        if (content.length > 0) {
            return this.replaceChars(new String(content, requestWrapper.getCharacterEncoding()));
        }
        return "";
    }

    public String replaceChars(String str) {
        for (Map.Entry<String, String> entry : replaceCharsError.entrySet()) {
            str = str.replace(entry.getKey(), entry.getValue());
        }
        return str;
    }
}
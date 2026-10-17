package com.hit.spring.web.filter;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestPaths {

    private RequestPaths() {
    }

    /**
     * Request URI without the servlet context path, the form used by {@code app.security.*} path lists.
     */
    public static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
    }
}

package com.hit.spring.security.authorization;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface AuthorizationHandler {

    boolean handle(HttpServletRequest request);

}

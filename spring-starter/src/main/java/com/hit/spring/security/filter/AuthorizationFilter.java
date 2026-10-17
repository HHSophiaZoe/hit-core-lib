package com.hit.spring.security.filter;

import com.hit.spring.web.filter.RequestPaths;
import com.hit.spring.security.SecurityProperties;
import com.hit.spring.exception.ResponseStatusCodeEnum;
import com.hit.spring.web.response.ResponseFactory;
import com.hit.spring.security.authorization.AuthorizationHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Asks the {@link AuthorizationHandler} whether the authenticated caller may call the request.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    private final AuthorizationHandler authorizationHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return securityProperties.isPublicPath(RequestPaths.pathWithinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean permitted;
        try {
            permitted = authorizationHandler.handle(request);
        } catch (Exception e) {
            log.error("Authorization check failed for {}", request.getRequestURI(), e);
            ResponseFactory.httpServletResponseToClient(response, ResponseStatusCodeEnum.INTERNAL_GENERAL_SERVER_ERROR);
            return;
        }
        if (!permitted) {
            ResponseFactory.httpServletResponseToClient(response, ResponseStatusCodeEnum.FORBIDDEN_ERROR);
            return;
        }
        filterChain.doFilter(request, response);
    }
}

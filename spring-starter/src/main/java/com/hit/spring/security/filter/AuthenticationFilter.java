package com.hit.spring.security.filter;

import com.hit.spring.web.filter.RequestPaths;
import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.security.SecurityProperties;
import com.hit.spring.security.SecurityContext;
import com.hit.spring.constant.CommonConstant;
import com.hit.spring.constant.HeaderConstant;
import com.hit.spring.exception.BaseResponseException;
import com.hit.spring.exception.ResponseStatusCodeEnum;
import com.hit.spring.web.response.ResponseFactory;
import com.hit.spring.security.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Reads the caller from the {@code Server-Token} header (system admin) or a {@code Bearer} token and stores it in
 * {@link SecurityContext} for the rest of the request.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final SecurityProperties securityProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return securityProperties.isPublicPath(RequestPaths.pathWithinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            this.authenticate(request);
        } catch (BaseResponseException e) {
            log.warn("Authentication failed for {}: {}", request.getRequestURI(), e.getResponseStatusCode().code());
            ResponseFactory.httpServletResponseToClient(response, e.getResponseStatusCode());
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clearContext();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String serverToken = request.getHeader(HeaderConstant.SERVER_TOKEN);
        if (StringUtils.isNotEmpty(serverToken)) {
            if (!this.isServerKey(serverToken)) {
                throw new BaseResponseException(ResponseStatusCodeEnum.UNAUTHORIZED_ERROR);
            }
            SecurityContext.setSimpleSecurityUser(SimpleSecurityUser.initSystemAdmin());
            return;
        }
        String userToken = request.getHeader(HeaderConstant.AUTHORIZATION);
        if (StringUtils.isNotEmpty(userToken) && userToken.startsWith(CommonConstant.BEARER_TOKEN)) {
            String token = userToken.substring(CommonConstant.BEARER_TOKEN.length()).trim();
            SimpleSecurityUser simpleSecurityUser = tokenService.readAccessToken(token)
                    .orElseThrow(() -> new BaseResponseException(ResponseStatusCodeEnum.UNAUTHORIZED_ERROR));
            SecurityContext.setAccessToken(token);
            SecurityContext.setSimpleSecurityUser(simpleSecurityUser);
        }
    }

    private boolean isServerKey(String serverToken) {
        return MessageDigest.isEqual(serverToken.getBytes(StandardCharsets.UTF_8),
                securityProperties.getServerKey().getBytes(StandardCharsets.UTF_8));
    }
}

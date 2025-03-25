package com.hit.spring.core.filter;

import com.hit.spring.config.properties.SecurityProperties;
import com.hit.spring.context.SecurityContext;
import com.hit.spring.core.constants.HeaderConstant;
import com.hit.spring.core.data.model.SimpleSecurityUser;
import com.hit.spring.core.exception.BaseResponseException;
import com.hit.spring.core.exception.BusinessException;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import com.hit.spring.core.factory.ResponseFactory;
import com.hit.spring.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final SecurityProperties securityProperties;

    @SneakyThrows
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            if (securityProperties.getApiWhitelist().stream().anyMatch(request.getRequestURI()::contains)) {
                log.debug("Api public: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String userToken = request.getHeader(HeaderConstant.AUTHORIZATION);
            String serverToken = request.getHeader(HeaderConstant.SERVER_TOKEN);
            if (StringUtils.isNotEmpty(serverToken)) {
                if (!serverToken.equalsIgnoreCase(securityProperties.getServerKey())) {
                    throw new BaseResponseException(ResponseStatusCodeEnum.UNAUTHORIZED_ERROR);
                }
                SecurityContext.setSimpleSecurityUser(SimpleSecurityUser.initSystemAdmin());
            } else if (StringUtils.isNotEmpty(userToken) && userToken.startsWith("Bearer")) {
                String token = userToken.replaceFirst("Bearer", "").trim();
                SimpleSecurityUser simpleSecurityUser = this.extractAuthentication(token);
                SecurityContext.setAccessToken(userToken);
                SecurityContext.setSimpleSecurityUser(simpleSecurityUser);
            }
            filterChain.doFilter(request, response);
        } catch (BaseResponseException e) {
            log.error("AuthenticationFilter ERROR", e);
            ResponseFactory.httpServletResponseToClient(response, e.getResponseStatusCode());
        } catch (Exception e) {
            log.error("AuthenticationFilter ERROR", e);
            ResponseFactory.httpServletResponseToClient(response, ResponseStatusCodeEnum.UNAUTHORIZED_ERROR);
        } finally {
            log.info("SecurityContext clear context");
            SecurityContext.clearContext();
        }
    }

    private SimpleSecurityUser extractAuthentication(String token) {
        try {
            SimpleSecurityUser user = jwtService.extractToken(token);
            if (user == null) {
                throw new BusinessException("SimpleSecurityUser is null");
            }
            return user;
        } catch (Exception e) {
            log.error("extractAuthentication ERROR: {}", e.getMessage(), e);
            throw new BaseResponseException(ResponseStatusCodeEnum.UNAUTHORIZED_ERROR);
        }
    }

}



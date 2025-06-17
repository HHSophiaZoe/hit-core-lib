package com.hit.spring.core.filter;

import com.hit.spring.config.properties.SecurityProperties;
import com.hit.spring.core.exception.BaseResponseException;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import com.hit.spring.core.factory.ResponseFactory;
import com.hit.spring.security.authorization.AuthorizationHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = {"app.security.filter.authorization"}, havingValue = "true")
public class AuthorizationFilter extends OncePerRequestFilter {

    @Setter(onMethod_ = {@Autowired})
    private SecurityProperties securityProperties;

    @Setter(onMethod_ = {@Autowired})
    private AuthorizationHandler authorizationHandler;

    @SneakyThrows
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            if (securityProperties.getApiWhitelist().stream().anyMatch(request.getRequestURI()::contains)) {
                log.debug("Api public: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            if (!authorizationHandler.handle(request)) {
                throw new BaseResponseException(ResponseStatusCodeEnum.FORBIDDEN_ERROR);
            }
            filterChain.doFilter(request, response);
        } catch (BaseResponseException e) {
            log.error("AuthorizationFilter error", e);
            ResponseFactory.httpServletResponseToClient(response, e.getResponseStatusCode());
        } catch (Exception e) {
            log.error("AuthorizationFilter error", e);
            ResponseFactory.httpServletResponseToClient(response, ResponseStatusCodeEnum.INTERNAL_GENERAL_SERVER_ERROR);
        }
    }

}



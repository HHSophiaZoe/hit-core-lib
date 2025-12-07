package com.hit.spring.security.annotation.resolver;

import com.hit.spring.context.SecurityContext;
import com.hit.common.model.SimpleSecurityUser;
import com.hit.spring.core.exception.BaseResponseException;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import com.hit.spring.security.annotation.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Configuration
public class RolesAllowedResolver {

    @Before("@annotation(rolesAllowed)")
    public void before(JoinPoint joinPoint, RolesAllowed rolesAllowed) {
        log.info("Before called {}", joinPoint.toString());
        boolean isValid = false;
        List<String> roles = Arrays.asList(rolesAllowed.role());
        SimpleSecurityUser simpleSecurityUser = SecurityContext.getSimpleSecurityUser();
        if (simpleSecurityUser == null) {
            throw new BaseResponseException(ResponseStatusCodeEnum.UNAUTHORIZED_ERROR);
        }
        if (simpleSecurityUser.isSystemAdmin()) {
            return;
        }
        List<String> authorities = simpleSecurityUser.getAuthorities();
        if (CollectionUtils.isNotEmpty(authorities)) {
            for (String authority : authorities) {
                if (roles.contains(authority)) {
                    isValid = true;
                    break;
                }
            }
        }
        if (!isValid) {
            throw new BaseResponseException(ResponseStatusCodeEnum.FORBIDDEN_ERROR);
        }
    }
}

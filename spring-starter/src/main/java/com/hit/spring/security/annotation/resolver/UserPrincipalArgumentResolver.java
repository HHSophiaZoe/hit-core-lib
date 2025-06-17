package com.hit.spring.security.annotation.resolver;

import com.hit.spring.context.SecurityContext;
import com.hit.spring.core.data.model.SimpleSecurityUser;
import com.hit.spring.core.data.model.UserPrincipal;
import com.hit.spring.core.exception.BaseResponseException;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import com.hit.spring.security.annotation.UserPrincipalRequest;
import com.hit.spring.util.IPAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class UserPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterAnnotation(UserPrincipalRequest.class) != null;
    }

    @Override
    public UserPrincipal resolveArgument(
            MethodParameter methodParameter,
            ModelAndViewContainer modelAndViewContainer,
            NativeWebRequest nativeWebRequest,
            WebDataBinderFactory webDataBinderFactory) {
        UserPrincipalRequest userPrincipalRequestAnnotation =
                methodParameter.getParameterAnnotation(UserPrincipalRequest.class);
        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();

        UserPrincipal userPrincipal = new UserPrincipal();
        userPrincipal.setIp(IPAddressUtils.getClientIpAddress(request));
        userPrincipal.setMethod(request.getMethod());
        userPrincipal.setUri(request.getRequestURI());

        SimpleSecurityUser simpleSecurityUser = SecurityContext.getSimpleSecurityUser();
        if (simpleSecurityUser == null && userPrincipalRequestAnnotation != null
                && userPrincipalRequestAnnotation.userRequired()) {
            throw new BaseResponseException(ResponseStatusCodeEnum.UNAUTHORIZED_ERROR);
        }
        userPrincipal.setUser(simpleSecurityUser);
        return userPrincipal;
    }
}

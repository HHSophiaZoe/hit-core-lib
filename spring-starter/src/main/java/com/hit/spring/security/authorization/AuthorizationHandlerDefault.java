package com.hit.spring.security.authorization;

import com.hit.spring.config.properties.SecurityProperties;
import com.hit.spring.context.SecurityContext;
import com.hit.spring.core.data.model.UserPrincipal;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import com.hit.spring.core.factory.GeneralResponse;
import com.hit.spring.core.factory.InternalResponse;
import com.hit.spring.service.http.HttpService;
import com.hit.spring.util.ApiUtils;
import com.hit.spring.util.DataUtils;
import com.hit.spring.util.IPAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(AuthorizationHandler.class)
@ConditionalOnProperty(value = {"app.security.filter.authorization"}, havingValue = "true")
public class AuthorizationHandlerDefault implements AuthorizationHandler {

    @Setter(onMethod_ = {@Autowired})
    private HttpService httpService;

    @Setter(onMethod_ = {@Autowired})
    private SecurityProperties securityProperties;

    @Override
    public boolean handle(HttpServletRequest request) {
        InternalResponse<GeneralResponse<Boolean>> httpResponse = ApiUtils.handleResponseInternal(() -> {
            ParameterizedTypeReference<GeneralResponse<Boolean>> responseType = new ParameterizedTypeReference<>() {
            };
            UserPrincipal userPrincipal = new UserPrincipal();
            userPrincipal.setIp(IPAddressUtils.getClientIpAddress(request));
            userPrincipal.setMethod(request.getMethod());
            userPrincipal.setUri(request.getRequestURI());
            userPrincipal.setUser(SecurityContext.getSimpleSecurityUser());
            return httpService.postBlocking(securityProperties.getFilter().getApiCheckPermissionUrl(), userPrincipal, new HttpHeaders(), responseType);
        });
        GeneralResponse<Boolean> isPermissionRes = httpResponse.getResponse();
        log.debug("Authorization is permission response: {}", DataUtils.parserLog(isPermissionRes));
        return ResponseStatusCodeEnum.SUCCESS.code().equals(isPermissionRes.getStatus().getCode()) && Boolean.TRUE.equals(isPermissionRes.getData());
    }
}

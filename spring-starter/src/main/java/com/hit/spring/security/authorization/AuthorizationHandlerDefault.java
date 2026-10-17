package com.hit.spring.security.authorization;

import com.hit.spring.security.SecurityProperties;
import com.hit.spring.security.SecurityContext;
import com.hit.common.model.UserPrincipal;
import com.hit.spring.exception.ResponseStatusCodeEnum;
import com.hit.spring.web.response.GeneralResponse;
import com.hit.spring.http.InternalResponse;
import com.hit.spring.http.HttpService;
import com.hit.spring.http.ApiUtils;
import com.hit.spring.util.DataUtils;
import com.hit.spring.web.IPAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

/**
 * Asks {@code app.security.filter.api-check-permission-url} whether the current user may call the request.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthorizationHandlerDefault implements AuthorizationHandler {

    private final HttpService httpService;

    private final SecurityProperties securityProperties;

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
            return httpService.post(securityProperties.getFilter().getApiCheckPermissionUrl(), userPrincipal, new HttpHeaders(), responseType);
        });
        GeneralResponse<Boolean> isPermissionRes = httpResponse.getResponse();
        log.debug("Authorization is permission response: {}", DataUtils.parserLog(isPermissionRes));
        return ResponseStatusCodeEnum.SUCCESS.code().equals(isPermissionRes.getStatus().getCode()) && Boolean.TRUE.equals(isPermissionRes.getData());
    }
}

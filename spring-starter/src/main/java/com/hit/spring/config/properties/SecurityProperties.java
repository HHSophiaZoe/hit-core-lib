package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Configuration
@ConfigurationProperties("app.security")
public class SecurityProperties {

    private Filter filter;

    private Set<String> apiWhitelist = new HashSet<>(List.of(
            "/swagger-ui", "/springdoc", "/v3/api-docs", "/actuator/health", "/auth/login"
    ));

    private String serverKey = "com.hit";

    private Jwt jwt;

    @Value("${app.security.apiWhitelist:}")
    public void setApiWhitelist(String[] apiWhitelistArray) {
        if (apiWhitelistArray != null && apiWhitelistArray.length > 0) {
            this.apiWhitelist.addAll(Arrays.asList(apiWhitelistArray));
        }
    }

    @Setter
    @Getter
    public static class Jwt {
        private String secretKey = "com.hit";
        private Integer accessExpire = 1440; // minutes
        private Integer refreshExpire = 60; // minutes
    }

    @Setter
    @Getter
    public static class Filter {
        private Boolean authentication = true;
        private Boolean authorization = false;
        private String apiCheckPermissionUrl;
    }

}

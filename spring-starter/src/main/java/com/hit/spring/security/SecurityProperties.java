package com.hit.spring.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@ConfigurationProperties("app.security")
public class SecurityProperties {

    private Filter filter = new Filter();

    private static final Set<String> DEFAULT_API_WHITELIST = Set.of(
            "/swagger-ui", "/springdoc", "/v3/api-docs", "/actuator/health", "/auth/login"
    );

    /**
     * Extra public paths of the application; they are added to {@link #DEFAULT_API_WHITELIST}.
     */
    private Set<String> apiWhitelist = new HashSet<>();

    private Set<String> apiSkipLogRequest =  new HashSet<>(List.of(
            "/swagger-ui", "/springdoc", "/v3/api-docs", "/actuator/health"
    ));

    private Set<String> sensitiveFieldLogRequest = new HashSet<>(List.of("password"));

    private String serverKey = "com.hit";

    private Jwt jwt = new Jwt();

    private Cors cors = new Cors();

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

    @Setter
    @Getter
    public static class Cors {
        private List<String> allowedOrigins = List.of("*");
        private List<String> allowedHeaders = List.of("*");
        private List<String> allowedMethods = List.of("*");
        private List<String> exposedHeaders;
        private Boolean allowCredentials;
        private Long maxAge = 3600L;
    }

    /**
     * Paths that skip authentication and authorization: the defaults plus {@code app.security.api-whitelist}.
     */
    public Set<String> getPublicPaths() {
        Set<String> paths = new HashSet<>(DEFAULT_API_WHITELIST);
        paths.addAll(apiWhitelist);
        return paths;
    }

    public boolean isPublicPath(String path) {
        return startsWithAny(path, this.getPublicPaths());
    }

    public boolean isSkipLogPath(String path) {
        return startsWithAny(path, apiSkipLogRequest);
    }

    private static boolean startsWithAny(String path, Set<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }
}

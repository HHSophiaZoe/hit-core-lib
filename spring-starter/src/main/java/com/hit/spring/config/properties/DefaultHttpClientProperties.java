package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("http-client.default")
public class DefaultHttpClientProperties {

    private Boolean enable = Boolean.TRUE;

    private ClientType type = ClientType.APACHE_HTTP_CLIENT;

    private Connection connection;

    private ConnectionPool connectionPool;

    @Setter
    @Getter
    public static class Connection {

        private int connectTimeout = 30;

        private int responseTimeout = 30;

        private int keepAlive = 20;

    }

    @Setter
    @Getter
    public static class ConnectionPool {

        private int maxTotal = 100; // Only applies to RestTemplate

        private int defaultMaxPerRoute = 10; // Only applies to RestTemplate

        private int maxIdle = 20; // Only applies to OkHttp

    }

    @Getter
    public enum ClientType {
        OK_HTTP, APACHE_HTTP_CLIENT
    }

}

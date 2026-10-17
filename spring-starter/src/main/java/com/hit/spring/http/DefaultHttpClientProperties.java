package com.hit.spring.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("http-client.default")
public class DefaultHttpClientProperties {

    private Boolean enable = Boolean.TRUE;

    private ClientType type = ClientType.APACHE_HTTP_CLIENT;

    private Connection connection = new Connection();

    private ConnectionPool connectionPool = new ConnectionPool();

    @Setter
    @Getter
    public static class Connection {

        private int connectRequestTimeout = 30; // Thời gian chờ lấy connection từ pool

        private int connectTimeout = 30; // Thời gian chờ bắt tay TCP (socket)

        private int responseTimeout = 30; // Thời gian chờ dữ liệu response sau khi kết nối

        private int keepAlive = 10;

    }

    @Setter
    @Getter
    public static class ConnectionPool {

        private int maxTotal = 1000;

        private int defaultMaxPerRoute = 100; // Request vượt số này sẽ chờ trong hàng đợi của pool

    }

    public enum ClientType {
        APACHE_HTTP_CLIENT
    }

}

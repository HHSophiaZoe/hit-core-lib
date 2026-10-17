package com.hit.spring.http;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds the shared {@link RestClient} on a pooled Apache HttpClient 5 from {@code http-client.default.*}.
 */
@RequiredArgsConstructor
public class DefaultHttpClientFactory {

    private final DefaultHttpClientProperties properties;

    public RestClient createRestClient() {
        return this.createRestClient(this.createRequestFactory());
    }

    public RestClient createRestClient(ClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .requestFactory(requestFactory)
                // Accept mặc định khi đọc body dạng String (như RestTemplate cũ); header Accept của caller vẫn ghi đè
                .defaultHeaders(headers -> headers.setAccept(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_YAML, new MediaType("application", "*+json"), MediaType.ALL)))
                .build();
    }

    public HttpComponentsClientHttpRequestFactory createRequestFactory() {
        DefaultHttpClientProperties.Connection connection = properties.getConnection();

        CloseableHttpClient closeableHttpClient = HttpClients.custom()
                .setConnectionManager(this.defaultHttpPoolingConnectionManager())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(connection.getConnectRequestTimeout(), TimeUnit.SECONDS)
                        .setResponseTimeout(connection.getResponseTimeout(), TimeUnit.SECONDS)
                        .setDefaultKeepAlive(connection.getKeepAlive(), TimeUnit.SECONDS)
                        .setCookieSpec(StandardCookieSpec.STRICT)
                        .build())
                .build();
        return new HttpComponentsClientHttpRequestFactory(closeableHttpClient);
    }

    private PoolingHttpClientConnectionManager defaultHttpPoolingConnectionManager() {
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(this.trustAllTlsStrategy())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(properties.getConnection().getConnectTimeout(), TimeUnit.SECONDS)
                        .build())
                .setMaxConnTotal(properties.getConnectionPool().getMaxTotal())
                .setMaxConnPerRoute(properties.getConnectionPool().getDefaultMaxPerRoute())
                .build();
    }

    @SneakyThrows
    private DefaultClientTlsStrategy trustAllTlsStrategy() {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();
        return new DefaultClientTlsStrategy(sslContext, HostnameVerificationPolicy.CLIENT,
                (s, sslSession) -> s.equalsIgnoreCase(sslSession.getPeerHost()));
    }

}

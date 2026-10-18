package com.hit.spring.autoconfigure;

import com.hit.spring.http.DefaultHttpClientFactory;
import com.hit.spring.http.DefaultHttpClientProperties;
import com.hit.spring.http.HttpService;
import com.hit.spring.http.RestClientHttpService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared {@code defaultRestClient} on pooled Apache HttpClient 5 and the {@link HttpService} over it.
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnBooleanProperty("http-client.default.enable")
@EnableConfigurationProperties(DefaultHttpClientProperties.class)
public class HitHttpClientAutoConfiguration {

    public static final String DEFAULT_REST_CLIENT = "defaultRestClient";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(name = DEFAULT_REST_CLIENT)
    static class DefaultRestClientConfiguration {

        @Lazy
        @Bean
        @ConditionalOnMissingBean(name = "defaultClientHttpRequestFactory")
        public HttpComponentsClientHttpRequestFactory defaultClientHttpRequestFactory(DefaultHttpClientProperties properties) {
            return new DefaultHttpClientFactory(properties).createRequestFactory();
        }

        @Lazy
        @Bean(name = DEFAULT_REST_CLIENT)
        @ConditionalOnMissingBean(name = DEFAULT_REST_CLIENT)
        public RestClient defaultRestClient(DefaultHttpClientProperties properties,
                                            @Qualifier("defaultClientHttpRequestFactory")
                                            HttpComponentsClientHttpRequestFactory requestFactory) {
            return new DefaultHttpClientFactory(properties).createRestClient(requestFactory);
        }
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean(HttpService.class)
    public HttpService httpService(@Qualifier(DEFAULT_REST_CLIENT) RestClient restClient, ObjectMapper objectMapper) {
        return new RestClientHttpService(restClient, objectMapper);
    }
}

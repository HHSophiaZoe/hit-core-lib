package com.hit.spring.autoconfigure;

import com.hit.spring.web.bind.PaginationArgumentResolver;
import com.hit.spring.ApplicationProperties;
import com.hit.spring.security.SecurityProperties;
import com.hit.spring.web.TomcatExecutorProperties;
import com.hit.spring.web.GlobalExceptionHandler;
import com.hit.spring.security.filter.AuthenticationFilter;
import com.hit.spring.security.filter.AuthorizationFilter;
import com.hit.spring.web.filter.LogCorrelationFilter;
import com.hit.spring.web.filter.RequestLoggingFilter;
import com.hit.spring.security.annotation.resolver.UserPrincipalArgumentResolver;
import com.hit.spring.security.authorization.AuthorizationHandler;
import com.hit.spring.security.authorization.AuthorizationHandlerDefault;
import com.hit.spring.http.HttpService;
import com.hit.spring.security.token.TokenService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.tomcat.autoconfigure.TomcatVirtualThreadsWebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Servlet web support: error handling, locale, argument resolvers, CORS, request filters, OpenAPI and the virtual
 * thread executor of Tomcat.
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({ApplicationProperties.class, SecurityProperties.class, TomcatExecutorProperties.class})
public class HitWebMvcAutoConfiguration {

    static final int CORS_FILTER_ORDER = 0;
    static final int LOG_CORRELATION_FILTER_ORDER = 1;
    static final int REQUEST_LOGGING_FILTER_ORDER = 2;
    static final int AUTHENTICATION_FILTER_ORDER = 3;
    static final int AUTHORIZATION_FILTER_ORDER = 4;

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * Locale from {@code Accept-Language}, Vietnamese when the header is absent.
     */
    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.forLanguageTag("vi-VN"));
        return localeResolver;
    }

    @Bean
    public WebMvcConfigurer hitArgumentResolverConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new UserPrincipalArgumentResolver());
                resolvers.add(new PaginationArgumentResolver());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "corsFilter")
    public FilterRegistrationBean<CorsFilter> corsFilter(SecurityProperties securityProperties) {
        SecurityProperties.Cors cors = securityProperties.getCors();
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(cors.getAllowedOrigins());
        corsConfig.setAllowedHeaders(cors.getAllowedHeaders());
        corsConfig.setAllowedMethods(cors.getAllowedMethods());
        corsConfig.setExposedHeaders(cors.getExposedHeaders());
        corsConfig.setAllowCredentials(cors.getAllowCredentials());
        corsConfig.setMaxAge(cors.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(CORS_FILTER_ORDER);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LogCorrelationFilter> logCorrelationFilter() {
        FilterRegistrationBean<LogCorrelationFilter> registration = new FilterRegistrationBean<>(new LogCorrelationFilter());
        registration.setOrder(LOG_CORRELATION_FILTER_ORDER);
        return registration;
    }

    @Bean
    @ConditionalOnBooleanProperty("app.enable-log-request-http")
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(
            ObjectProvider<RequestLoggingFilter.ActionLogRequest> actionLogRequest, SecurityProperties securityProperties) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(
                new RequestLoggingFilter(actionLogRequest.getIfAvailable(), securityProperties));
        registration.setOrder(REQUEST_LOGGING_FILTER_ORDER);
        return registration;
    }

    @Bean
    @ConditionalOnBooleanProperty("app.security.filter.authentication")
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilter(TokenService tokenService,
                                                                             SecurityProperties securityProperties) {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>(
                new AuthenticationFilter(tokenService, securityProperties));
        registration.setOrder(AUTHENTICATION_FILTER_ORDER);
        return registration;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty("app.security.filter.authorization")
    static class AuthorizationConfiguration {

        @Bean
        @ConditionalOnMissingBean(AuthorizationHandler.class)
        public AuthorizationHandlerDefault authorizationHandler(HttpService httpService, SecurityProperties securityProperties) {
            return new AuthorizationHandlerDefault(httpService, securityProperties);
        }

        @Bean
        public FilterRegistrationBean<AuthorizationFilter> authorizationFilter(SecurityProperties securityProperties,
                                                                               AuthorizationHandler authorizationHandler) {
            FilterRegistrationBean<AuthorizationFilter> registration = new FilterRegistrationBean<>(
                    new AuthorizationFilter(securityProperties, authorizationHandler));
            registration.setOrder(AUTHORIZATION_FILTER_ORDER);
            return registration;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(GroupedOpenApi.class)
    static class OpenApiConfiguration {

        private static final String API_KEY = "Bearer Token";

        @Bean
        @ConditionalOnMissingBean(GroupedOpenApi.class)
        public GroupedOpenApi publicApi() {
            return GroupedOpenApi.builder()
                    .group("public-apis")
                    .pathsToMatch("/**")
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean(OpenAPI.class)
        public OpenAPI customOpenAPI(ApplicationProperties applicationProperties) {
            return new OpenAPI()
                    .info(new Info()
                            .title(applicationProperties.getName() + " API")
                            .version("1.0"))
                    .components(new Components().addSecuritySchemes(API_KEY, new SecurityScheme()
                            .name("Authorization")
                            .scheme("Bearer")
                            .bearerFormat("JWT")
                            .type(SecurityScheme.Type.APIKEY)
                            .in(SecurityScheme.In.HEADER)))
                    .addSecurityItem(new SecurityRequirement().addList(API_KEY));
        }
    }

    /**
     * Tomcat handles requests on named virtual threads with a cap on concurrent requests.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(TomcatVirtualThreadsWebServerFactoryCustomizer.class)
    @ConditionalOnThreading(Threading.VIRTUAL)
    static class TomcatVirtualThreadsConfiguration {

        @Bean
        public TomcatVirtualThreadsWebServerFactoryCustomizer tomcatVirtualThreadsWebServerFactoryCustomizer(TomcatExecutorProperties properties) {
            return new TomcatVirtualThreadsWebServerFactoryCustomizer() {
                @Override
                public void customize(ConfigurableTomcatWebServerFactory factory) {
                    factory.addProtocolHandlerCustomizers(protocolHandler -> {
                        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(properties.getThreadNamePrefix());
                        executor.setVirtualThreads(true);
                        executor.setConcurrencyLimit(properties.getConcurrencyLimit());
                        protocolHandler.setExecutor(executor);
                    });
                }

                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }
            };
        }
    }
}

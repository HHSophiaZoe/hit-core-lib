package com.hit.spring.autoconfigure;

import com.hit.spring.tracing.LogTrackingProcessor;
import com.hit.spring.i18n.Translator;
import com.hit.spring.ApplicationProperties;
import com.hit.spring.security.SecurityProperties;
import com.hit.spring.convert.DataConverter;
import com.hit.spring.event.EventPublisher;
import com.hit.spring.event.EventPublisherImpl;
import com.hit.spring.json.JsonMapper;
import com.hit.spring.security.annotation.resolver.RolesAllowedResolver;
import com.hit.spring.security.token.TokenService;
import com.hit.spring.security.token.jwt.JwtTokenService;
import com.hit.spring.util.BeanUtils;
import com.hit.spring.util.EnvironmentUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.convert.ConversionService;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * Core beans of the hit starters: static holders ({@code BeanUtils}, {@code EnvironmentUtils}, {@code JsonMapper},
 * {@code Translator}), events, message bundles, aspects and the {@link TokenService}.
 */
@EnableConfigurationProperties({ApplicationProperties.class, SecurityProperties.class})
@AutoConfiguration(after = JacksonAutoConfiguration.class, before = MessageSourceAutoConfiguration.class)
public class HitSpringAutoConfiguration {

    /**
     * Bundle of the starters; the application {@code messages} bundle is read first and may override its codes.
     */
    public static final String HIT_MESSAGES_BASENAME = "hit-messages";

    // Static callers do not trigger lazy bean creation; initialize the context holder at startup.
    @Bean
    public static BeanUtils hitBeanUtils() {
        return new BeanUtils();
    }

    @Bean
    public static EnvironmentUtils hitEnvironmentUtils() {
        return new EnvironmentUtils();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonMapper hitJsonMapper(ObjectMapper objectMapper) {
        return new JsonMapper(objectMapper);
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher eventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new EventPublisherImpl(applicationEventPublisher);
    }

    @Lazy
    @Bean
    @ConditionalOnMissingBean
    public DataConverter dataConverter(@Qualifier("mvcConversionService") ObjectProvider<ConversionService> mvcConversionService) {
        return new DataConverter(mvcConversionService.getIfAvailable(ApplicationConversionService::getSharedInstance));
    }

    @Bean
    @ConditionalOnMissingBean(name = "messageSource")
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages", HIT_MESSAGES_BASENAME);
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public Translator hitTranslator() {
        return new Translator();
    }

    @Bean
    public LogTrackingProcessor logTrackingProcessor() {
        return new LogTrackingProcessor();
    }

    @Bean
    public RolesAllowedResolver rolesAllowedResolver() {
        return new RolesAllowedResolver();
    }

    /**
     * JWT tokens when a secret is configured and jjwt is on the classpath; an application {@link TokenService} bean replaces them.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
    @ConditionalOnProperty("app.security.jwt.secret-key")
    static class JwtTokenConfiguration {

        @Bean
        @Lazy
        @ConditionalOnMissingBean(TokenService.class)
        public TokenService tokenService(ObjectMapper objectMapper, SecurityProperties securityProperties) {
            return new JwtTokenService(objectMapper, securityProperties);
        }
    }
}

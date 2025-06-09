package com.hit.spring.core.filter;

import com.hit.spring.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@RequiredArgsConstructor
public class RegistrationFilter {

    private final SecurityProperties securityProperties;

    @Bean
    @Primary
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(securityProperties.getCors().getAllowedOrigins());
        corsConfig.setAllowedHeaders(securityProperties.getCors().getAllowedHeaders());
        corsConfig.setAllowedMethods(securityProperties.getCors().getAllowedMethods());
        corsConfig.setAllowCredentials(securityProperties.getCors().getAllowCredentials());
        if (CollectionUtils.isNotEmpty(securityProperties.getCors().getExposedHeaders())) {
            corsConfig.setExposedHeaders(securityProperties.getCors().getExposedHeaders());
        }
        if (securityProperties.getCors().getAllowCredentials() != null) {
            corsConfig.setAllowCredentials(securityProperties.getCors().getAllowCredentials());
        }
        corsConfig.setMaxAge(securityProperties.getCors().getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }
}

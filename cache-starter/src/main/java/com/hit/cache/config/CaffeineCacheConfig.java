package com.hit.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hit.cache.config.properties.CacheConfigProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
@ConditionalOnProperty(value = {"cache.internal.enable"}, havingValue = "true")
public class CaffeineCacheConfig {


    @Bean("internalCaffeineConfig")
    public Caffeine<Object, Object> caffeineConfig(CacheConfigProperties properties) {
        return Caffeine.from(properties.getInternal().getCaffeine().getSpec());
    }

    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager(@Qualifier("internalCaffeineConfig") Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

}

package com.hit.cache.autoconfigure;

import com.hit.cache.config.properties.CacheConfigProperties;
import com.hit.cache.helper.CacheKeyGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Internal (Caffeine/Ehcache, {@code cache.internal.*}) and external (Redis through Redisson, {@code cache.external.*})
 * caches. Runs before Boot's cache (and Redis, when the application adds it) auto-configuration so they back off for the
 * beans defined here.
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class, before = CacheAutoConfiguration.class,
        beforeName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@EnableConfigurationProperties(CacheConfigProperties.class)
@Import({InternalCacheConfiguration.class, ExternalCacheConfiguration.class})
public class HitCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheKeyGenerator cacheKeyGenerator(CacheConfigProperties properties) {
        return new CacheKeyGenerator(properties);
    }
}

package com.hit.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hit.cache.annotation.ConditionalOnInternalCacheEnable;
import com.hit.cache.config.properties.CacheConfigProperties;
import com.hit.cache.config.properties.CacheConfigProperties.InternalCacheConfigProperties;
import com.hit.cache.config.properties.CacheConfigProperties.InternalCacheConfigProperties.CacheType;
import lombok.RequiredArgsConstructor;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import java.io.IOException;

@EnableCaching
@Configuration
@RequiredArgsConstructor
@ConditionalOnInternalCacheEnable
public class InternalCacheConfig {

    private final CacheConfigProperties properties;

    @Bean("internalCacheManager")
    public CacheManager caffeineCacheManager() throws IOException {
        InternalCacheConfigProperties internalProps = properties.getInternal();
        if (CacheType.CAFFEINE.equals(internalProps.getCacheType())) {
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            caffeineCacheManager.setCaffeine(Caffeine.from(internalProps.getCaffeine().getSpec()));
            return caffeineCacheManager;
        } else if (CacheType.EHCACHE.equals(internalProps.getCacheType())) {
            EhcacheCachingProvider ehcacheCachingProvider = (EhcacheCachingProvider) Caching.getCachingProvider(EhcacheCachingProvider.class.getName());
            javax.cache.CacheManager manager = ehcacheCachingProvider.getCacheManager(
                    internalProps.getEhCache().getConfig().getURI(),
                    getClass().getClassLoader()
            );
            JCacheCacheManager jCacheCacheManager = new JCacheCacheManager();
            jCacheCacheManager.setCacheManager(manager);
            return jCacheCacheManager;
        } else {
            return new NoOpCacheManager();
        }
    }

}

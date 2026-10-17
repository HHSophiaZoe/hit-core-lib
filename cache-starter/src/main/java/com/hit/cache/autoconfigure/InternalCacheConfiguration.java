package com.hit.cache.autoconfigure;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hit.cache.annotation.ConditionalOnInternalCacheEnable;
import com.hit.cache.config.properties.CacheConfigProperties;
import com.hit.cache.config.properties.CacheConfigProperties.InternalCacheConfigProperties;
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
@ConditionalOnInternalCacheEnable
@Configuration(proxyBeanMethods = false)
class InternalCacheConfiguration {

    @Bean("internalCacheManager")
    public CacheManager internalCacheManager(CacheConfigProperties properties) throws IOException {
        InternalCacheConfigProperties internal = properties.getInternal();
        if (internal.getCacheType() == null) {
            return new NoOpCacheManager();
        }
        return switch (internal.getCacheType()) {
            case CAFFEINE -> {
                CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
                caffeineCacheManager.setAllowNullValues(properties.getAllowNullValues());
                caffeineCacheManager.setCaffeine(Caffeine.from(internal.getCaffeine().getSpec()));
                yield caffeineCacheManager;
            }
            case EHCACHE -> {
                EhcacheCachingProvider provider =
                        (EhcacheCachingProvider) Caching.getCachingProvider(EhcacheCachingProvider.class.getName());
                JCacheCacheManager jCacheCacheManager = new JCacheCacheManager(provider.getCacheManager(
                        internal.getEhcache().getConfig().getURI(), InternalCacheConfiguration.class.getClassLoader()));
                jCacheCacheManager.setAllowNullValues(properties.getAllowNullValues());
                yield jCacheCacheManager;
            }
            case NONE -> new NoOpCacheManager();
        };
    }
}

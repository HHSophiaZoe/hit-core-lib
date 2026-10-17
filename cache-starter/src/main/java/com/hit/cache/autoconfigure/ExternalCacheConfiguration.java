package com.hit.cache.autoconfigure;

import com.hit.cache.annotation.ConditionalOnExternalCacheEnable;
import com.hit.cache.config.properties.CacheConfigProperties;
import com.hit.cache.config.serializer.RedisSerializer;
import com.hit.cache.config.serializer.RedisSerializerImpl;
import com.hit.cache.helper.CacheKeyGenerator;
import com.hit.cache.helper.DistributedAtomic;
import com.hit.cache.helper.RedisDistribution;
import com.hit.cache.lock.processor.DistributedLockSimpleLockProcessor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableCaching
@ConditionalOnExternalCacheEnable
@Configuration(proxyBeanMethods = false)
class ExternalCacheConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(CacheConfigProperties properties) {
        log.info("Create RedissonClient");
        return Redisson.create(properties.getExternal().getRedisson());
    }

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(RedisSerializer.class)
    public RedisSerializer hitRedisSerializer(ObjectMapper objectMapper) {
        return new RedisSerializerImpl(objectMapper);
    }

    @Primary
    @Bean("externalCacheManager")
    public CacheManager externalCacheManager(RedisConnectionFactory redisConnectionFactory, CacheConfigProperties properties,
                                             RedisSerializer redisSerializer) {
        CacheConfigProperties.ExternalCacheConfigProperties external = properties.getExternal();
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        external.getCacheExpirations().forEach((cacheName, ttlSeconds) ->
                cacheConfigurations.put(cacheName, cacheConfiguration(properties, redisSerializer, ttlSeconds)));
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration(properties, redisSerializer, external.getDefaultExpireSec()))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(DistributedAtomic.class)
    public RedisDistribution redisDistribution(StringRedisTemplate stringRedisTemplate) {
        return new RedisDistribution(stringRedisTemplate);
    }

    @Bean
    public DistributedLockSimpleLockProcessor distributedLockSimpleLockProcessor(DistributedAtomic distributedAtomic,
                                                                                 CacheKeyGenerator cacheKeyGenerator) {
        return new DistributedLockSimpleLockProcessor(distributedAtomic, cacheKeyGenerator);
    }

    private static RedisCacheConfiguration cacheConfiguration(CacheConfigProperties properties, RedisSerializer serializer,
                                                              long ttlSeconds) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .computePrefixWith(cacheName -> {
                    String prefix = properties.getAppCache() + properties.getDelimiter();
                    if (!cacheName.isEmpty()) {
                        prefix += cacheName + properties.getDelimiter();
                    }
                    return prefix;
                })
                .entryTtl(Duration.ofSeconds(ttlSeconds));
        if (Boolean.FALSE.equals(properties.getAllowNullValues())) {
            configuration = configuration.disableCachingNullValues();
        }
        return configuration;
    }
}

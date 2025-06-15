package com.hit.cache.store.external;

import com.hit.cache.config.properties.ExternalCacheConfigProperties;
import com.hit.cache.config.serializer.RedisSerializer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
public abstract class RedisCacheStoreImpl implements BaseExternalCacheStore {

    @Setter(onMethod_ = {@Autowired})
    private RedisTemplate<String, String> stringRedisTemplate;

    @Setter(onMethod_ = {@Autowired})
    private ExternalCacheConfigProperties cacheConfigProp;

    @Setter(onMethod_ = {@Autowired})
    private RedissonClient redissonClient;

    @Setter(onMethod_ = {@Autowired})
    private RedisSerializer redisSerializer;

    private String keyGen(String key) {
        return this.cacheConfigProp.getApplicationCache() + this.cacheConfigProp.getDelimiter() + key;
    }

    @Override
    public <T> void putObject(String key, T value) {
        this.putObject(key, value, cacheConfigProp.getCacheDefaultExpire());
    }

    @Override
    public <T> void putObject(String key, T value, long expire) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache put: key = {}, value = {}, expire = {}", keyGen, value, expire);
        ValueOperations<String, String> ops = this.stringRedisTemplate.opsForValue();
        ops.set(keyGen, redisSerializer.serializer(value));
        this.stringRedisTemplate.expire(keyGen, Duration.ofSeconds(expire));
    }

    @Override
    public <T> T getObject(String key, Class<T> objectClass) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache get: key = {}", keyGen);
        String valueStr = this.stringRedisTemplate.opsForValue().get(keyGen);
        return redisSerializer.deserializer(valueStr, objectClass);
    }

    @Override
    public <K, V> void putObjectAsHash(String key, Map<K, V> value) {
        this.putObjectAsHash(key, value, this.cacheConfigProp.getCacheDefaultExpire());
    }

    @Override
    public <K, V> void putObjectAsHash(String key, Map<K, V> value, long expire) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache put: key = {}, value = {}", keyGen, value);
        byte[] rawKey = redisSerializer.serializerRaw(keyGen);
        Map<byte[], byte[]> entries = HashMap.newHashMap(value.size());
        for (Map.Entry<K, V> entry : value.entrySet()) {
            byte[] hashKeyRaw = redisSerializer.serializerRaw(entry.getKey());
            byte[] hashValueRaw = redisSerializer.serializerRaw(entry.getValue());
            entries.put(hashKeyRaw, hashValueRaw);
        }
        this.stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
            try (connection) {
                connection.hMSet(rawKey, entries);
                connection.expire(rawKey, expire);
            }
            return null;
        });
    }

    @Override
    public <K, V> void putObjectToHash(String key, K hashKey, V hashValue) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache put: key = {}, hashKey = {}, hashValue = {}", keyGen, hashKey, hashValue);
        this.stringRedisTemplate.opsForHash()
                .put(keyGen, redisSerializer.serializer(hashKey), redisSerializer.serializer(hashValue));
    }

    @Override
    public <K, V> Map<K, V> getObjectAsHash(String key, Class<K> objectClassKey,
                                                                          Class<V> objectClassValue) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCacheTemplate get: key = {}", keyGen);
        Map<byte[], byte[]> entries =
                this.stringRedisTemplate.execute((RedisCallback<Map<byte[], byte[]>>) connection -> {
                    return connection.hGetAll(redisSerializer.serializerRaw(keyGen));
                });
        if (entries == null || entries.isEmpty()) {
            log.debug("Key {} does not exist", keyGen);
            return Collections.emptyMap();
        }
        Map<K, V> hashes = HashMap.newHashMap(entries.size());
        for (Map.Entry<byte[], byte[]> entry : entries.entrySet()) {
            K deserializeHashKey = redisSerializer.deserializerRaw(entry.getKey(), objectClassKey);
            V deserializeHashValue = redisSerializer.deserializerRaw(entry.getValue(), objectClassValue);
            hashes.put(deserializeHashKey, deserializeHashValue);
        }
        return hashes;
    }

    @Override
    public <K, V> V getObjectFromHash(String key, K hashKey, Class<V> objectClassValue) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache get: key = {}, hashKey = {}", keyGen, hashKey);
        String hashValueStr = (String) this.stringRedisTemplate.opsForHash()
                .get(keyGen, redisSerializer.serializer(hashKey));
        return redisSerializer.deserializer(hashValueStr, objectClassValue);
    }

    @Override
    @SafeVarargs
    public final <K> void deleteHashValue(String key, K... hashKeys) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache del: key = {}, hashKeys = {}", keyGen, hashKeys);
        Object[] hashKeysStr = Arrays.stream(hashKeys).map(redisSerializer::serializer).toArray(Object[]::new);
        this.stringRedisTemplate.opsForHash().delete(keyGen, hashKeysStr);
    }

    @Override
    public void delete(String key) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache delete: key = {}", keyGen);
        stringRedisTemplate.delete(keyGen);
    }

    @Override
    public boolean hasKey(String key) {
        try {
            String keyGen = this.keyGen(key);
            return this.stringRedisTemplate.hasKey(keyGen);
        } catch (Exception e) {
            return false;
        }
    }

    public void watchKey(String key) {
        String keyGen = this.keyGen(key);
        this.stringRedisTemplate.watch(keyGen);
    }

}

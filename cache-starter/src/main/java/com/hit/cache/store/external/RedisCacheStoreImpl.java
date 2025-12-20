package com.hit.cache.store.external;

import com.hit.cache.config.properties.CacheConfigProperties;
import com.hit.cache.config.serializer.RedisSerializer;
import com.hit.cache.helper.CacheContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class RedisCacheStoreImpl implements BaseExternalCacheStore {

    @Setter(onMethod_ = {@Autowired})
    protected StringRedisTemplate redisTemplate;

    @Setter(onMethod_ = {@Autowired})
    protected CacheConfigProperties cacheConfigProp;

    @Setter(onMethod_ = {@Autowired})
    protected RedisSerializer redisSerializer;

    protected static final String OK = "OK";

    protected String keyGen(String key) {
        return this.cacheConfigProp.getAppCache() + this.cacheConfigProp.getDelimiter() + key;
    }

    @Override
    public <T> void put(String key, T value) {
        this.put(key, value, cacheConfigProp.getExternal().getDefaultExpireSec());
    }

    @Override
    public <T> void put(String key, T value, long expire) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache put: key = {}, value = {}, expire = {}", keyGen, value, expire);
        ValueOperations<String, String> ops = this.redisTemplate.opsForValue();
        ops.set(keyGen, redisSerializer.serializeToJson(value));
        this.redisTemplate.expire(keyGen, Duration.ofSeconds(expire));
    }

    @Override
    public <T> boolean putIfAbsent(String key, T value, long expireSeconds) {
        try {
            return OK.equals(redisTemplate.execute(
                    CacheContext.getSetIfAbsentScript(), Collections.singletonList(this.keyGen(key)),
                    redisSerializer.serializeToJson(value), String.valueOf(expireSeconds)
            ));
        } catch (Exception ex) {
            log.error("error when call redis", ex);
            return false;
        }
    }

    @Override
    public <T> T get(String key, Class<T> objectClass) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache get: key = {}", keyGen);
        String valueStr = this.redisTemplate.opsForValue().get(keyGen);
        return redisSerializer.deserialize(valueStr, objectClass);
    }

    @Override
    public <K, V> void putAsHash(String key, Map<K, V> value) {
        this.putAsHash(key, value, this.cacheConfigProp.getExternal().getDefaultExpireSec());
    }

    @Override
    public <K, V> void putAsHash(String key, Map<K, V> value, long expire) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache put: key = {}, value = {}", keyGen, value);
        byte[] rawKey = redisSerializer.serializeToRawJson(keyGen);
        Map<byte[], byte[]> entries = HashMap.newHashMap(value.size());
        for (Map.Entry<K, V> entry : value.entrySet()) {
            byte[] hashKeyRaw = redisSerializer.serializeToRawJson(entry.getKey());
            byte[] hashValueRaw = redisSerializer.serializeToRawJson(entry.getValue());
            entries.put(hashKeyRaw, hashValueRaw);
        }
        this.redisTemplate.execute((RedisCallback<Object>) connection -> {
            try (connection) {
                connection.hMSet(rawKey, entries);
                connection.expire(rawKey, expire);
            }
            return null;
        });
    }

    @Override
    public <K, V> void putToHash(String key, K hashKey, V hashValue) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache put: key = {}, hashKey = {}, hashValue = {}", keyGen, hashKey, hashValue);
        this.redisTemplate.opsForHash()
                .put(keyGen, redisSerializer.serializeToJson(hashKey), redisSerializer.serializeToJson(hashValue));
    }

    @Override
    public <K, V> Map<K, V> getAsHash(String key, Class<K> objectClassKey,
                                      Class<V> objectClassValue) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCacheTemplate get: key = {}", keyGen);
        Map<byte[], byte[]> entries =
                this.redisTemplate.execute((RedisCallback<Map<byte[], byte[]>>) connection -> {
                    return connection.hGetAll(redisSerializer.serializeToRawJson(keyGen));
                });
        if (entries == null || entries.isEmpty()) {
            log.debug("Key {} does not exist", keyGen);
            return Collections.emptyMap();
        }
        Map<K, V> hashes = HashMap.newHashMap(entries.size());
        for (Map.Entry<byte[], byte[]> entry : entries.entrySet()) {
            K deserializeHashKey = redisSerializer.deserializeRaw(entry.getKey(), objectClassKey);
            V deserializeHashValue = redisSerializer.deserializeRaw(entry.getValue(), objectClassValue);
            hashes.put(deserializeHashKey, deserializeHashValue);
        }
        return hashes;
    }

    @Override
    public <K, V> V getFromHash(String key, K hashKey, Class<V> objectClassValue) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache get: key = {}, hashKey = {}", keyGen, hashKey);
        String hashValueStr = (String) this.redisTemplate.opsForHash()
                .get(keyGen, redisSerializer.serializeToJson(hashKey));
        return redisSerializer.deserialize(hashValueStr, objectClassValue);
    }

    @Override
    @SafeVarargs
    public final <K> void delHashValue(String key, K... hashKeys) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache del: key = {}, hashKeys = {}", keyGen, hashKeys);
        Object[] hashKeysStr = Arrays.stream(hashKeys).map(redisSerializer::serializeToJson).toArray(Object[]::new);
        this.redisTemplate.opsForHash().delete(keyGen, hashKeysStr);
    }

    @Override
    public void delete(String key) {
        String keyGen = this.keyGen(key);
        log.debug("RedisCache delete: key = {}", keyGen);
        redisTemplate.delete(keyGen);
    }

    @Override
    public boolean hasKey(String key) {
        try {
            String keyGen = this.keyGen(key);
            return this.redisTemplate.hasKey(keyGen);
        } catch (Exception e) {
            return false;
        }
    }

}

package com.hit.cache.store.internal;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Slf4j
public abstract class InternalCacheStoreImpl implements BaseInternalCacheStore {

    @Setter(onMethod_ = {@Autowired, @Qualifier("internalCacheManager")})
    protected CacheManager internalCacheManager;

    @Override
    public void putAll(String cacheName, Map<Object, Object> data) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache != null) data.forEach(cache::put);
    }

    @Override
    public void put(String cacheName, Object k, Object v) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache != null) cache.put(k, v);
    }

    @Override
    public <T> List<T> getAll(String cacheName, Class<T> type) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache == null) return List.of();

        Object nativeCache = cache.getNativeCache();
        List<T> values = new ArrayList<>();
        if (nativeCache instanceof javax.cache.Cache<?, ?> jCache) {
            for (javax.cache.Cache.Entry<?, ?> entry : jCache) {
                values.add(type.cast(entry.getValue()));
            }
            return values;
        }
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
            caffeineCache.asMap().values().forEach(value -> values.add(type.cast(value)));
            return values;
        }
        throw new IllegalStateException("Unsupported internal cache provider: " + nativeCache.getClass().getName());
    }

    @Override
    public <T> T get(String cacheName, Object key, Class<T> type) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache != null) return cache.get(key, type);
        return null;
    }

    @Override
    public <T, R> R getAndMap(String cacheName, Object key, Class<T> type, Function<? super T, ? extends R> mapper) {
        Cache cache = internalCacheManager.getCache(cacheName);
        return mapper.apply(cache != null ? cache.get(key, type) : null);
    }

    @Override
    public <T> T computeAndPut(String cacheName, Object key, Class<T> type, UnaryOperator<T> valueProvider) {
        Cache cache = internalCacheManager.getCache(cacheName);
        T cacheValue = cache != null ? cache.get(key, type) : null;
        T value = valueProvider.apply(cacheValue);
        if (value != null) this.put(cacheName, key, value);
        return value;
    }

    @Override
    public void deleteCache(String cacheName) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache != null) {
            log.info("Delete cache: {}", cacheName);
            cache.clear();
        }
    }

    @Override
    public void deleteKey(String cacheName, Object key) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache != null) {
            log.info("Delete cache: {}, key: {}", cacheName, key);
            cache.evict(key);
        }
    }

    @Override
    public void deleteAll() {
        internalCacheManager.getCacheNames().forEach(this::deleteCache);
    }
}

package com.hit.cache.store.internal;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Map;
import java.util.function.Function;

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
    public <T> T get(String cacheName, Object key, Class<T> type) {
        Cache cache = internalCacheManager.getCache(cacheName);
        if (cache != null) return cache.get(key, type);
        return null;
    }

    @Override
    public <T, R> R get(String cacheName, Object key, Class<T> type,
                        Function<? super T, ? extends R> handleCache) {
        Cache cache = internalCacheManager.getCache(cacheName);
        return handleCache.apply(cache != null ? cache.get(key, type) : null);
    }

    @Override
    public <T, R> R getAndPut(String cacheName, Object key, Class<T> type,
                              Function<? super T, ? extends R> handleCache) {
        Cache cache = internalCacheManager.getCache(cacheName);
        T cacheValue = cache != null ? cache.get(key, type) : null;
        R value = handleCache.apply(cacheValue);
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
    public void deleteKey(String cacheName, String key) {
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

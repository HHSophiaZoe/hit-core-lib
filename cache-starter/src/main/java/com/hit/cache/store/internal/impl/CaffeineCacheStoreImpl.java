package com.hit.cache.store.internal.impl;

import com.hit.cache.store.internal.InternalCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service("caffeineCacheStore")
@ConditionalOnProperty(value = {"internal-cache.enable"}, havingValue = "true")
public class CaffeineCacheStoreImpl implements InternalCacheStore {

    @Qualifier("caffeineCacheManager")
    private final CacheManager caffeineCacheManager;

    @Override
    public void putAll(String cacheName, Map<Object, Object> data) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) data.forEach(cache::put);
    }

    @Override
    public void put(String cacheName, Object k, Object v) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) cache.put(k, v);
    }

    @Override
    public <T> T get(String cacheName, Object key, Class<T> type) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) return cache.get(key, type);
        return null;
    }

    @Override
    public <T, R> R get(String cacheName, Object key, Class<T> type,
                        Function<? super T, ? extends R> handleCache) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        return handleCache.apply(cache != null ? cache.get(key, type) : null);
    }

    @Override
    public <T, R> R getAndPut(String cacheName, Object key, Class<T> type,
                              Function<? super T, ? extends R> handleCache) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        T cacheValue = cache != null ? cache.get(key, type) : null;
        R value = handleCache.apply(cacheValue);
        if (value != null) this.put(cacheName, key, value);
        return value;
    }

    @Override
    public void deleteCache(String cacheName) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }

    @Override
    public void deleteKey(String cacheName, String key) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
    }
}

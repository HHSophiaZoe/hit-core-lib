package com.hit.cache.store.internal;

import java.util.Map;
import java.util.function.Function;

public interface BaseInternalCacheStore {

    void putAll(String cacheName, Map<Object, Object> data);

    void put(String cacheName, Object key, Object v);

    <T> T get(String cacheName, Object key, Class<T> type);

    <T, R> R get(String cacheName, Object key, Class<T> type, Function<? super T, ? extends R> handleCache);

    <T, R> R getAndPut(String cacheName, Object key, Class<T> type, Function<? super T, ? extends R> handleCache);

    void deleteCache(String cacheName);

    void deleteKey(String cacheName, String key);

}

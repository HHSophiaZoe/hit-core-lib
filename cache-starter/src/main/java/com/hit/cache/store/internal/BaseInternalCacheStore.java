package com.hit.cache.store.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface BaseInternalCacheStore {

    void putAll(String cacheName, Map<Object, Object> data);

    void put(String cacheName, Object key, Object v);

    <T> List<T> getAll(String cacheName, Class<T> type);

    <T> T get(String cacheName, Object key, Class<T> type);

    <T, R> R getAndMap(String cacheName, Object key, Class<T> type, Function<? super T, ? extends R> mapper);

    <T> T computeAndPut(String cacheName, Object key, Class<T> type, UnaryOperator<T> valueProvider);

    void deleteCache(String cacheName);

    void deleteKey(String cacheName, Object key);

    void deleteAll();

}

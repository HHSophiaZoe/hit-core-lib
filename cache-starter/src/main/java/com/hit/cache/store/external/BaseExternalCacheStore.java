package com.hit.cache.store.external;

import java.util.Map;

public interface BaseExternalCacheStore {

    <T> void put(String key, T value);

    <T> void put(String key, T value, long expireSeconds);

    <T> boolean putIfAbsent(String key, T value, long expireSeconds);

    <T> T get(String key, Class<T> objectClass);

    <K, V> void putAsHash(String key, Map<K, V> value);

    <K, V> void putAsHash(String key, Map<K, V> value, long expireSeconds);

    <K, V> void putToHash(String key, K hashKey, V hashValue);

    <K, V> Map<K, V> getAsHash(String key, Class<K> objectClassKey, Class<V> objectClassValue);

    <K, V> V getFromHash(String key, K hashKey, Class<V> objectClassValue);

    <K> void delHashValue(String key, K... hashKeys);

    void delete(String key);

    boolean hasKey(String key);

}

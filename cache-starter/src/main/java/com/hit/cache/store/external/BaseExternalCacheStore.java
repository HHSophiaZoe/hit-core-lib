package com.hit.cache.store.external;

import java.util.Map;

public interface BaseExternalCacheStore {

    <T> void putObject(String key, T value);

    <T> void putObject(String key, T value, long expireSeconds);

    <T> T getObject(String key, Class<T> objectClass);

    <K, V> void putObjectAsHash(String key, Map<K, V> value);

    <K, V> void putObjectAsHash(String key, Map<K, V> value, long expireSeconds);

    <K, V> void putObjectToHash(String key, K hashKey, V hashValue);

    <K, V> Map<K, V> getObjectAsHash(String key, Class<K> objectClassKey, Class<V> objectClassValue);

    <K, V> V getObjectFromHash(String key, K hashKey, Class<V> objectClassValue);

    <K> void deleteHashValue(String key, K... hashKeys);

    void delete(String key);

    boolean hasKey(String key);

    void watchKey(String key);

}

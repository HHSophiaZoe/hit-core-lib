package com.hit.cache.store.internal;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalCacheStoreImplTest {

    @Test
    void shouldReadAllValuesAndDeleteObjectKeyFromCaffeine() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(10));
        InternalCacheStoreImpl store = store(cacheManager);
        UUID key = UUID.randomUUID();

        store.put("jobs", key, "completed");

        assertEquals(List.of("completed"), store.getAll("jobs", String.class));
        store.deleteKey("jobs", key);
        assertNull(store.get("jobs", key, String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReadAllValuesFromJCache() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache springCache = mock(Cache.class);
        javax.cache.Cache<Object, Object> jCache = mock(javax.cache.Cache.class);
        javax.cache.Cache.Entry<Object, Object> entry = mock(javax.cache.Cache.Entry.class);
        when(cacheManager.getCache("jobs")).thenReturn(springCache);
        when(springCache.getNativeCache()).thenReturn(jCache);
        when(entry.getValue()).thenReturn("failed");
        when(jCache.iterator()).thenReturn(List.of(entry).iterator());
        InternalCacheStoreImpl store = store(cacheManager);

        assertEquals(List.of("failed"), store.getAll("jobs", String.class));
    }

    private InternalCacheStoreImpl store(CacheManager cacheManager) {
        InternalCacheStoreImpl store = new InternalCacheStoreImpl() {
        };
        store.internalCacheManager = cacheManager;
        return store;
    }
}

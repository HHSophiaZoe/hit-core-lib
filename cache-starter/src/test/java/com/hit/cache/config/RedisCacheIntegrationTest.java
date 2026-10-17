package com.hit.cache.config;

import com.hit.cache.lock.DistributedLock;
import com.hit.cache.store.external.RedisCacheStoreImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** cache-starter qua auto-configuration, Redisson 4 / Spring Data Redis 4 với Redis thật (Testcontainers), serialize bằng Jackson 3. */
@Testcontainers
@SpringBootTest(classes = RedisCacheIntegrationTest.Beans.class, properties = {
        "cache.app-cache=it",
        "cache.external.enable=true",
        "cache.external.default-expire-sec=60"
})
class RedisCacheIntegrationTest {

    // Started and closed by the @Testcontainers extension
    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("cache.external.redisson.single-server-config.address",
                () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    record Quote(String symbol, Instant at, LocalDate tradingDate, long price) {
    }

    @Autowired
    private TestStore store;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LockedJob lockedJob;

    @Autowired
    @Qualifier("externalCacheManager")
    private CacheManager cacheManager;

    @Test
    void storePutsAndReadsTypedValueWithTtl() {
        Quote quote = new Quote("FPT", Instant.parse("2026-10-17T02:15:00Z"), LocalDate.of(2026, 10, 17), 125_000);

        store.put("quote:FPT", quote);

        assertThat(store.get("quote:FPT", Quote.class)).isEqualTo(quote);
        assertThat(redisTemplate.opsForValue().get("it::quote:FPT"))
                .isEqualTo("{\"symbol\":\"FPT\",\"at\":\"2026-10-17T02:15:00Z\",\"tradingDate\":\"2026-10-17\",\"price\":125000}");
        assertThat(redisTemplate.getExpire("it::quote:FPT")).isBetween(1L, 60L);
    }

    @Test
    void putIfAbsentOnlyWritesOnce() {
        assertThat(store.putIfAbsent("lock:job", "a", 30)).isTrue();
        assertThat(store.putIfAbsent("lock:job", "b", 30)).isFalse();
        assertThat(store.get("lock:job", String.class)).isEqualTo("a");
    }

    @Test
    void hashRoundTrip() {
        store.putAsHash("prices", Map.of("FPT", 125_000L, "VNM", 61_000L));
        store.putToHash("prices", "HPG", 27_000L);

        assertThat(store.getAsHash("prices", String.class, Long.class))
                .containsEntry("FPT", 125_000L).containsEntry("VNM", 61_000L).containsEntry("HPG", 27_000L);
        assertThat(store.getFromHash("prices", "HPG", Long.class)).isEqualTo(27_000L);
    }

    @Test
    void springCacheManagerStoresThroughRedis() {
        Cache cache = cacheManager.getCache("tickers");
        cache.put("FPT", Map.of("price", 125_000));

        assertThat(cache.get("FPT").get()).isEqualTo(Map.of("price", 125_000));
        assertThat(redisTemplate.hasKey("it::tickers::FPT")).isTrue();
    }

    @Test
    void distributedLockLetsOneCallerInAndTimesOutTheOther() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> holder = CompletableFuture.supplyAsync(() -> lockedJob.run("order-1", entered, release));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        // Same key while held: waits waitTimeMs then gives up (returns null without running)
        assertThat(lockedJob.run("order-1", new CountDownLatch(1), new CountDownLatch(0))).isNull();
        // Another key is not blocked
        assertThat(lockedJob.run("order-2", new CountDownLatch(1), new CountDownLatch(0))).isEqualTo("done order-2");

        release.countDown();
        assertThat(holder.get(5, TimeUnit.SECONDS)).isEqualTo("done order-1");
        assertThat(lockedJob.run("order-1", new CountDownLatch(1), new CountDownLatch(0))).isEqualTo("done order-1");
        assertThat(redisTemplate.hasKey("it::job::order-1")).isFalse();
    }

    static class TestStore extends RedisCacheStoreImpl {
    }

    static class LockedJob {

        @DistributedLock(name = "job", value = "#key", waitTimeMs = 300, waitIntervalMs = 100)
        public String run(String key, CountDownLatch entered, CountDownLatch release) {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "done " + key;
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Beans {

        @Bean
        TestStore testStore() {
            return new TestStore();
        }

        @Bean
        LockedJob lockedJob() {
            return new LockedJob();
        }
    }
}

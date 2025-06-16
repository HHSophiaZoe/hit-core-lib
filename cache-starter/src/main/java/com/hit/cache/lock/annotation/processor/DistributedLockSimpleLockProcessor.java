package com.hit.cache.lock.annotation.processor;

import com.github.f4b6a3.ulid.UlidCreator;
import com.hit.cache.lock.helper.DistributedAtomic;
import com.hit.cache.lock.annotation.DistributedLock;
import com.hit.cache.lock.exception.DistributedLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Aspect
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = {"external-cache.enable"}, havingValue = "true")
public class DistributedLockSimpleLockProcessor extends DistributedLockAbstractProcessor {

    private final DistributedAtomic distributedAtomic;

    private static final String READ_LOCK_SUFFIX = "READ_LOCK";

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        if (!distributedLock.enabled()) return pjp.proceed();

        String name = distributedLock.name();
        String value = this.getValue(pjp, distributedLock.value());
        boolean readLock = distributedLock.readLock();
        long transactionTtlSec = distributedLock.transactionTtlSec();
        long waitTimeMs = distributedLock.waitTimeMs();
        long waitIntervalMs = distributedLock.waitIntervalMs();

        if (StringUtils.isEmpty(value)) return pjp.proceed();

        String key = this.buildKey(name, value);
        long startTime = System.currentTimeMillis();
        do {
            String uniqueValue = UlidCreator.getMonotonicUlid().toString();
            long startCallRedisTime = System.currentTimeMillis();
            if (this.acquireLock(key, uniqueValue, transactionTtlSec, readLock)) {
                log.info("DistributedLock: got key '{}' after {} ms, read-lock {}", key, System.currentTimeMillis() - startTime, readLock);
                try {
                    return pjp.proceed();
                } finally {
                    if (this.unlock(key, uniqueValue, readLock)) {
                        log.info("DistributedLock: key '{}' unlocked, read-lock {}", key, readLock);
                    } else {
                        log.warn("DistributedLock: cannot del key {}, read-lock {}", key, readLock);
                    }
                }
            }
            if (waitIntervalMs <= 0) {
                log.info("zero acquire lock interval");
                break;
            }
            long callRedisTime = System.currentTimeMillis() - startCallRedisTime;
            if (callRedisTime < waitIntervalMs) {
                Thread.sleep(waitIntervalMs - callRedisTime);
                waitTimeMs -= waitIntervalMs;
            } else {
                waitTimeMs -= callRedisTime;
            }
        } while (waitTimeMs > 0);

        log.warn("DistributedLock timeout: cannot wait for key '{}' after {} ms", value, waitTimeMs);
        if (distributedLock.exception() != DistributedLockException.None.class) {
            throw distributedLock.exception().getDeclaredConstructor(DistributedLockException.DistributedLockData.class)
                    .newInstance(new DistributedLockException.DistributedLockData(name, value));
        }
        return null;
    }

    private boolean unlock(String key, String val, boolean readLock) {
        String suffix = readLock ? READ_LOCK_SUFFIX : StringUtils.EMPTY;
        return Boolean.TRUE.equals(distributedAtomic.deleteKeyVal(key, val + suffix));
    }

    private boolean acquireLock(String key, String value, long ttl, boolean readLock) { // Tạo đc key -> trả về true
        return readLock
                ? distributedAtomic.setIfAbsentWithSuffix(key, value, ttl, READ_LOCK_SUFFIX) // Nếu có chứa READ_LOCK_SUFFIX thì tạo key mới
                : distributedAtomic.setIfAbsent(key, value, ttl);
    }
}

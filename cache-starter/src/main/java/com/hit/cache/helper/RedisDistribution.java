package com.hit.cache.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class RedisDistribution implements DistributedAtomic {

    private final StringRedisTemplate redisTemplate;

    private static final String OK = "OK";

    @Override
    public boolean setIfAbsent(String key, String val, long ttl) {
        try {
            return OK.equals(redisTemplate.execute(RedisScripts.SET_IF_ABSENT, Collections.singletonList(key), val, String.valueOf(ttl)));
        } catch (Exception ex) {
            log.error("error when call redis --> skip lock: ", ex);
            return true;
        }
    }

    @Override
    public boolean setIfAbsentWithSuffix(String key, String val, long ttl, String suffix) {
        try {
            return OK.equals(redisTemplate.execute(RedisScripts.SET_IF_ABSENT_WITH_SUFFIX, Collections.singletonList(key), val, String.valueOf(ttl), suffix));
        } catch (Exception ex) {
            log.error("error when call redis --> skip lock: ", ex);
            return true;
        }
    }

    @Override
    public Boolean deleteKeyVal(String key, String val) {
        try {
            return redisTemplate.execute(RedisScripts.DELETE_KEY_VALUE, Collections.singletonList(key), val);
        } catch (Exception ex) {
            log.error("error when call redis --> skip del key: ", ex);
            return false;
        }
    }
}

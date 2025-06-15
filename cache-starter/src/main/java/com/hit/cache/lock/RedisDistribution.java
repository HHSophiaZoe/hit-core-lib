package com.hit.cache.lock;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = {"external-cache.enable"}, havingValue = "true")
public class RedisDistribution implements DistributedAtomic {

    private final StringRedisTemplate redisTemplate;

    private RedisScript<Boolean> deleteKeyScript;
    private RedisScript<String> setIfAbsentScript;
    private RedisScript<String> setIfAbsentWithSuffixScript;
    private static final String OK = "OK";

    @PostConstruct
    public void init() throws IOException {
        ScriptSource delScriptSource = new ResourceScriptSource(new ClassPathResource("deleteByKeyValue.lua"));
        deleteKeyScript = RedisScript.of(delScriptSource.getScriptAsString(), Boolean.class);
        ScriptSource setScriptSource = new ResourceScriptSource(new ClassPathResource("setIfAbsent.lua"));
        setIfAbsentScript = RedisScript.of(setScriptSource.getScriptAsString(), String.class);
        ScriptSource setWithSuffixScript = new ResourceScriptSource(new ClassPathResource("setIfAbsentWithSuffix.lua"));
        setIfAbsentWithSuffixScript = RedisScript.of(setWithSuffixScript.getScriptAsString(), String.class);
    }

    @Override
    public boolean setIfAbsent(String key, String val, long ttl) {
        try {
            return OK.equals(redisTemplate.execute(setIfAbsentScript, Collections.singletonList(key), val, String.valueOf(ttl)));
        } catch (Exception ex) {
            log.error("error when call redis --> skip lock: ", ex);
            return true;
        }
    }

    @Override
    public boolean setIfAbsentWithSuffix(String key, String val, long ttl, String suffix) {
        try {
            return OK.equals(redisTemplate.execute(setIfAbsentWithSuffixScript, Collections.singletonList(key), val, String.valueOf(ttl), suffix));
        } catch (Exception ex) {
            log.error("error when call redis --> skip lock: ", ex);
            return true;
        }
    }

    @Override
    public Boolean deleteKeyVal(String key, String val) {
        try {
            return redisTemplate.execute(deleteKeyScript, Collections.singletonList(key), val);
        } catch (Exception ex) {
            log.error("error when call redis --> skip del key: ", ex);
            return false;
        }
    }
}

package com.hit.cache.helper;

import com.hit.cache.config.properties.CacheConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CacheContext {

    @Setter
    @Getter
    private static CacheConfigProperties cacheConfigProperties;

    @Getter
    private static RedisScript<Boolean> deleteKeyScript;

    @Getter
    private static RedisScript<String> setIfAbsentScript;

    @Getter
    private static RedisScript<String> setIfAbsentWithSuffixScript;

    @PostConstruct
    public void init() throws IOException {
        ScriptSource delScriptSource = new ResourceScriptSource(new ClassPathResource("deleteByKeyValue.lua"));
        deleteKeyScript = RedisScript.of(delScriptSource.getScriptAsString(), Boolean.class);
        ScriptSource setScriptSource = new ResourceScriptSource(new ClassPathResource("setIfAbsent.lua"));
        setIfAbsentScript = RedisScript.of(setScriptSource.getScriptAsString(), String.class);
        ScriptSource setWithSuffixScript = new ResourceScriptSource(new ClassPathResource("setIfAbsentWithSuffix.lua"));
        setIfAbsentWithSuffixScript = RedisScript.of(setWithSuffixScript.getScriptAsString(), String.class);
    }

    @Autowired
    CacheContext(CacheConfigProperties cacheConfigProperties) {
        setCacheConfigProperties(cacheConfigProperties);
    }

}

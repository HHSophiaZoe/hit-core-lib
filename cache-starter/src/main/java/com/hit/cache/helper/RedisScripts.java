package com.hit.cache.helper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Lua scripts of the distributed lock and {@code putIfAbsent}; each script is read from the classpath once.
 */
public final class RedisScripts {

    public static final RedisScript<Boolean> DELETE_KEY_VALUE =
            RedisScript.of(new ClassPathResource("hit-cache/deleteByKeyValue.lua"), Boolean.class);

    public static final RedisScript<String> SET_IF_ABSENT =
            RedisScript.of(new ClassPathResource("hit-cache/setIfAbsent.lua"), String.class);

    public static final RedisScript<String> SET_IF_ABSENT_WITH_SUFFIX =
            RedisScript.of(new ClassPathResource("hit-cache/setIfAbsentWithSuffix.lua"), String.class);

    private RedisScripts() {
    }
}

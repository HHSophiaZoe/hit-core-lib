package com.hit.cache.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Matches when {@code cache.external.enable=true} (Redis through Redisson).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnBooleanProperty("cache.external.enable")
public @interface ConditionalOnExternalCacheEnable {
}

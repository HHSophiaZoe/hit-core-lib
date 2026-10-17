package com.hit.cache.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Matches when {@code cache.internal.enable=true} (Caffeine or Ehcache in the JVM).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnBooleanProperty("cache.internal.enable")
public @interface ConditionalOnInternalCacheEnable {
}

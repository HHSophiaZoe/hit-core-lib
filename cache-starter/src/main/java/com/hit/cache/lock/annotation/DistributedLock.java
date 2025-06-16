package com.hit.cache.lock.annotation;

import com.hit.cache.lock.exception.DistributedLockException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String name();

    String value() default "";

    Class<? extends DistributedLockException> exception() default DistributedLockException.None.class;

    boolean enabled() default true;

    boolean readLock() default false;

    long transactionTtlSec() default 30L;

    long waitTimeMs() default 1000L; // Max total lock wait time

    long waitIntervalMs() default 500L; // Wait time between retries

}

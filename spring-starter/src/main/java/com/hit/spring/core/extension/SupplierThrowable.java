package com.hit.spring.core.extension;

@FunctionalInterface
public interface SupplierThrowable<T> {

    T get() throws Exception;

}

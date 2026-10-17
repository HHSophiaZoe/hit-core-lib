package com.hit.spring.http;

@FunctionalInterface
public interface SupplierThrowable<T> {

    T get() throws Exception;

}

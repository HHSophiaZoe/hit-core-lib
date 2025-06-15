package com.hit.spring.core.reactive;

public interface Emitter<T> {
    void onNext(T chunk);
    void onError(Throwable error);
    void onComplete();
}
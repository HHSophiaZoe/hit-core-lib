package com.hit.spring.core.extension;

public interface Emitter<T> {
    void onNext(T chunk);
    void onError(Throwable error);
    void onComplete();
}
package com.hit.spring.reactive;


public interface Emitter<T> {
    void onNext(T chunk);
    void onError(StreamingException error);
    void onComplete();
}
package com.hit.spring.core.reactive;

import com.hit.spring.core.exception.StreamingException;

public interface Emitter<T> {
    void onNext(T chunk);
    void onError(StreamingException error);
    void onComplete();
}
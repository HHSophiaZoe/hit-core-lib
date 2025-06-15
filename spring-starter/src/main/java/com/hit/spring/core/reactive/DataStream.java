package com.hit.spring.core.reactive;

import java.util.function.Consumer;

public class DataStream<T> {

    private final DataStreamSource<T> source;

    private DataStream(DataStreamSource<T> source) {
        this.source = source;
    }

    public static <T> DataStream<T> create(DataStreamSource<T> source) {
        return new DataStream<>(source);
    }

    public void subscribe(Consumer<T> onNext, Consumer<Throwable> onError, Runnable onComplete) {
        source.subscribe(new Emitter<>() {
            @Override
            public void onNext(T chunk) {
                onNext.accept(chunk);
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }

            @Override
            public void onComplete() {
                onComplete.run();
            }
        });
    }

    public interface DataStreamSource<T> {
        void subscribe(Emitter<T> emitter);
    }
}

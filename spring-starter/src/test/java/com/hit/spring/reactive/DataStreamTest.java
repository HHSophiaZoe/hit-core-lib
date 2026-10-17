package com.hit.spring.reactive;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DataStreamTest {

    @Test
    void subscriberReceivesEveryChunkThenCompletion() {
        List<String> received = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();

        DataStream.<String>create(emitter -> {
            List.of("A", "B", "C").forEach(emitter::onNext);
            emitter.onComplete();
        }).subscribe(received::add, error -> {
            throw new AssertionError(error);
        }, () -> completed.set(true));

        assertThat(received).containsExactly("A", "B", "C");
        assertThat(completed).isTrue();
    }

    @Test
    void sourceErrorReachesTheSubscriber() {
        AtomicReference<Throwable> error = new AtomicReference<>();

        DataStream.<String>create(emitter -> emitter.onError(new StreamingException("source failed", new IllegalStateException())))
                .subscribe(chunk -> {
                }, error::set, () -> {
                });

        assertThat(error.get()).isInstanceOf(StreamingException.class).hasMessage("source failed");
    }
}

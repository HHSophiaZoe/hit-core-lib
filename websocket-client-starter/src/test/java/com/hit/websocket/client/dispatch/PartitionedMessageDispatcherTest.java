package com.hit.websocket.client.dispatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionedMessageDispatcherTest {

    @Test
    void closeDiscardsQueuedTasksAndRejectsNewOnes() throws Exception {
        AtomicInteger dropped = new AtomicInteger();
        MessageDispatcherObserver observer = new MessageDispatcherObserver() {
            @Override public void dropped(DispatchContext context) { dropped.incrementAndGet(); }
        };
        MessageDispatcherOptions options = new MessageDispatcherOptions("test-close", 1, 1, DispatchOverflowPolicy.FAIL);
        MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of(observer)).create(options);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch hold = new CountDownLatch(1);
        try {
            dispatcher.dispatch("key", () -> {
                entered.countDown();
                await(hold);
            });
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            dispatcher.dispatch("key", () -> { });
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> dispatcher.dispatch("key", () -> { }))
                    .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
            dispatcher.close();
            assertThat(dropped).hasValue(1);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> dispatcher.dispatch("key", () -> { }))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            hold.countDown();
            dispatcher.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesOrderForTheSameOrderingKey(boolean virtualThreads) throws InterruptedException {
        MessageDispatcherOptions options = new MessageDispatcherOptions(
                "test", 2, 10, DispatchOverflowPolicy.DROP_OLDEST);
        ThreadFactory threads = virtualThreads ? Thread.ofVirtual().factory() : Thread.ofPlatform().daemon().factory();
        MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of(), threads).create(options);
        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch completed = new CountDownLatch(3);

        dispatcher.dispatch("FPT", () -> record(processed, completed, 1));
        dispatcher.dispatch("FPT", () -> record(processed, completed, 2));
        dispatcher.dispatch("FPT", () -> record(processed, completed, 3));

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(processed).containsExactly(1, 2, 3);
        dispatcher.close();
    }

    @Test
    void dropsOldestQueuedMessageWhenPartitionIsFull() throws InterruptedException {
        AtomicInteger dropped = new AtomicInteger();
        MessageDispatcherObserver observer = new MessageDispatcherObserver() {
            @Override
            public void dropped(DispatchContext context) {
                dropped.incrementAndGet();
            }
        };
        MessageDispatcherOptions options = new MessageDispatcherOptions(
                "test", 1, 1, DispatchOverflowPolicy.DROP_OLDEST);
        MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of(observer)).create(options);
        List<Integer> processed = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);

        dispatcher.dispatch("FPT", () -> {
            firstStarted.countDown();
            await(releaseFirst);
            record(processed, completed, 1);
        });
        assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();
        dispatcher.dispatch("FPT", () -> record(processed, completed, 2));
        dispatcher.dispatch("FPT", () -> record(processed, completed, 3));
        releaseFirst.countDown();

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(processed).containsExactly(1, 3);
        assertThat(dropped).hasValue(1);
        dispatcher.close();
    }

    @Test
    void observerFailureDoesNotStopDispatcherWorkers() throws InterruptedException {
        MessageDispatcherObserver failingObserver = new MessageDispatcherObserver() {
            @Override
            public void queueRegistered(String dispatcher, int partition, IntSupplier queueSize) {
                throw new IllegalStateException("queue metric failed");
            }

            @Override
            public void processed(DispatchContext context, Duration duration) {
                throw new IllegalStateException("processing metric failed");
            }
        };
        MessageDispatcherOptions options = new MessageDispatcherOptions(
                "test", 1, 10, DispatchOverflowPolicy.DROP_OLDEST);
        MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of(failingObserver)).create(options);
        CountDownLatch completed = new CountDownLatch(2);

        dispatcher.dispatch("FPT", completed::countDown);
        dispatcher.dispatch("FPT", completed::countDown);

        assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        dispatcher.close();
    }

    @Test
    void concurrentProducersDropExactlyOnceWithoutRetryLoop() throws Exception {
        AtomicInteger dropped = new AtomicInteger();
        MessageDispatcherObserver observer = new MessageDispatcherObserver() {
            @Override public void dropped(DispatchContext context) { dropped.incrementAndGet(); }
        };
        MessageDispatcherOptions options = new MessageDispatcherOptions("concurrent", 1, 1, DispatchOverflowPolicy.DROP_OLDEST);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch hold = new CountDownLatch(1);
        try (MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of(observer), Thread.ofVirtual().factory()).create(options)) {
            dispatcher.dispatch("key", () -> {
                entered.countDown();
                try {
                    hold.await();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            try (ExecutorService producers = Executors.newVirtualThreadPerTaskExecutor()) {
                List<java.util.concurrent.Future<?>> submitted = new ArrayList<>();
                for (int index = 0; index < 500; index++) {
                    submitted.add(producers.submit(() -> dispatcher.dispatch("key", () -> { })));
                }
                for (java.util.concurrent.Future<?> result : submitted) result.get(5, TimeUnit.SECONDS);
            }
            assertThat(dropped).hasValue(499);
            dispatcher.close();
            assertThat(dropped).hasValue(500);
        } finally {
            hold.countDown();
        }
    }

    @Test
    void dropLatestKeepsPreviouslyQueuedMessage() throws Exception {
        AtomicInteger dropped = new AtomicInteger();
        MessageDispatcherObserver observer = new MessageDispatcherObserver() {
            @Override public void dropped(DispatchContext context) { dropped.incrementAndGet(); }
        };
        MessageDispatcherOptions options = new MessageDispatcherOptions("latest", 1, 1, DispatchOverflowPolicy.DROP_LATEST);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch hold = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        try (MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of(observer)).create(options)) {
            dispatcher.dispatch("key", () -> { entered.countDown(); await(hold); });
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            dispatcher.dispatch("key", completed::countDown);
            dispatcher.dispatch("key", () -> { throw new AssertionError("Dropped task must not run"); });
            assertThat(dropped).hasValue(1);
            hold.countDown();
            assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            hold.countDown();
        }
    }

    private static void record(List<Integer> processed, CountDownLatch completed, int value) {
        processed.add(value);
        completed.countDown();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }
}

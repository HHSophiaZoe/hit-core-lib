package com.hit.websocket.client.dispatch;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionedMessageDispatcherTest {

    @Test
    void closeReleasesBlockedProducerAndRejectsFurtherDispatch() throws Exception {
        MessageDispatcherOptions options = new MessageDispatcherOptions("test-close", 1, 1, DispatchOverflowPolicy.BLOCK);
        MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of()).create(options);
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch holdWorker = new CountDownLatch(1);
        java.util.concurrent.ExecutorService producer = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            dispatcher.dispatch("FPT", () -> {
                workerStarted.countDown();
                await(holdWorker);
            });
            assertThat(workerStarted.await(2, TimeUnit.SECONDS)).isTrue();
            dispatcher.dispatch("FPT", () -> { });
            CountDownLatch producerStarted = new CountDownLatch(1);
            java.util.concurrent.Future<?> blocked = producer.submit(() -> {
                producerStarted.countDown();
                dispatcher.dispatch("FPT", () -> { });
            });
            assertThat(producerStarted.await(2, TimeUnit.SECONDS)).isTrue();
            dispatcher.close();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> blocked.get(2, TimeUnit.SECONDS))
                    .isInstanceOf(java.util.concurrent.ExecutionException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> dispatcher.dispatch("FPT", () -> { }))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            holdWorker.countDown();
            dispatcher.close();
            producer.shutdownNow();
        }
    }

    @Test
    void preservesOrderForTheSameOrderingKey() throws InterruptedException {
        MessageDispatcherOptions options = new MessageDispatcherOptions(
                "test", 2, 10, DispatchOverflowPolicy.DROP_OLDEST);
        MessageDispatcher dispatcher = new MessageDispatcherFactory(List.of()).create(options);
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

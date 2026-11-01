package com.hit.spring.task;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutorServiceFactoryTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsSingleWorkerWithConfiguredThreadType(boolean virtual) throws Exception {
        try (ExecutorService executor = new ExecutorServiceFactory(virtual).newSingleThreadExecutor("single-test-")) {
            assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isEqualTo(virtual);
            assertThat(executor.submit(() -> Thread.currentThread().getName()).get()).startsWith("single-test-");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsFixedPoolWithConfiguredThreadType(boolean virtual) throws Exception {
        try (ExecutorService executor = new ExecutorServiceFactory(virtual).newFixedThreadPool(2, "pool-test-")) {
            assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isEqualTo(virtual);
            assertThat(executor.submit(() -> Thread.currentThread().getName()).get()).startsWith("pool-test-");
        }
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsScheduledWorkerWithConfiguredThreadType(boolean virtual) throws Exception {
        try (ScheduledExecutorService executor = new ExecutorServiceFactory(virtual)
                .newSingleThreadScheduledExecutor("scheduler-test-", true)) {
            assertThat(executor.schedule(() -> Thread.currentThread().isVirtual(), 0, TimeUnit.MILLISECONDS).get()).isEqualTo(virtual);
            assertThat(executor.submit(() -> Thread.currentThread().isDaemon()).get()).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsThreadPerTaskWithConfiguredThreadType(boolean virtual) throws Exception {
        try (ExecutorService executor = new ExecutorServiceFactory(virtual).newThreadPerTaskExecutor("task-test-")) {
            assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isEqualTo(virtual);
            Thread first = executor.submit(Thread::currentThread).get();
            Thread second = executor.submit(Thread::currentThread).get();
            assertThat(first).isNotSameAs(second);
        }
    }
}

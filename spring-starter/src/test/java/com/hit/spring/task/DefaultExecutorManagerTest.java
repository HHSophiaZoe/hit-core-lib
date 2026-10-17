package com.hit.spring.task;

import com.hit.spring.tracing.TrackingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultExecutorManagerTest {

    private final SimpleAsyncTaskExecutor executor = virtualExecutor();

    private final ExecutorManager manager = new DefaultExecutorManager(executor, Duration.ofSeconds(2));

    @AfterEach
    void clearTrace() {
        TrackingContext.clearContext();
        executor.close();
    }

    @Test
    void zipTasksWaitsForAllTasksIncludingWrappedOnes() {
        List<String> done = new CopyOnWriteArrayList<>();

        manager.zipTasks(() -> done.add("plain"), new RunnableWrapper(() -> done.add("wrapped")));

        assertThat(done).containsExactlyInAnyOrder("plain", "wrapped");
    }

    @Test
    void defaultStrategyWaitsForAllThenThrowsTheFailure() {
        AtomicBoolean slowFinished = new AtomicBoolean();

        assertThatThrownBy(() -> manager.zipTasks(
                () -> {
                    throw new IllegalStateException("boom");
                },
                () -> {
                    sleep(300);
                    slowFinished.set(true);
                }))
                .isInstanceOf(ExecutorException.class)
                .hasMessageContaining("boom")
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(slowFinished).isTrue();
    }

    @Test
    void errorCancelFailsFastAndInterruptsTheOthers() throws InterruptedException {
        CountDownLatch interrupted = new CountDownLatch(1);
        long start = System.nanoTime();

        assertThatThrownBy(() -> manager.zipTasks(ExecutorManager.FailureStrategy.ERROR_CANCEL,
                () -> {
                    sleep(100);
                    throw new IllegalStateException("first failure");
                },
                () -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(10));
                    } catch (InterruptedException e) {
                        interrupted.countDown();
                    }
                }))
                .isInstanceOf(ExecutorException.class)
                .hasMessageContaining("first failure");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void ignoreErrorSwallowsFailures() {
        AtomicBoolean other = new AtomicBoolean();

        manager.zipTasks(ExecutorManager.FailureStrategy.IGNORE_ERROR,
                () -> {
                    throw new IllegalStateException("ignored");
                },
                () -> other.set(true));

        assertThat(other).isTrue();
    }

    @Test
    void timeoutInterruptsUnfinishedTasks() {
        ExecutorManager shortTimeout = new DefaultExecutorManager(executor, Duration.ofMillis(200));

        assertThatThrownBy(() -> shortTimeout.zipTasks(() -> sleep(5_000)))
                .isInstanceOf(ExecutorException.class)
                .hasMessage("Task execution timeout");
    }

    @Test
    void zipTasksReturnCombinesResultsAndRethrowsTaskFailure() {
        assertThat(manager.zipTasksReturn(() -> 2, () -> 3, Integer::sum)).isEqualTo(5);

        assertThatThrownBy(() -> manager.zipTasksReturn(() -> 1, () -> {
            throw new IllegalArgumentException("bad input");
        }, (a, b) -> a))
                .isInstanceOf(ExecutorException.class)
                .hasMessage("Task execution failed: bad input")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tasksRunWithTheCallerTraceIdAndCallerKeepsItsOwn() {
        TrackingContext.setTraceId("caller-trace");
        List<String> seen = new CopyOnWriteArrayList<>();

        manager.zipTasks(() -> seen.add(TrackingContext.currentTraceId()));

        assertThat(seen).containsExactly("caller-trace");
        assertThat(TrackingContext.currentTraceId()).isEqualTo("caller-trace");
    }

    @Test
    void wrapperRunOnTheCallerThreadRestoresItsTraceId() {
        TrackingContext.setTraceId("outer");
        RunnableWrapper wrapper = new RunnableWrapper(() -> assertThat(TrackingContext.currentTraceId()).isEqualTo("outer"));
        TrackingContext.setTraceId("current");

        wrapper.run();

        assertThat(TrackingContext.currentTraceId()).isEqualTo("current");
    }

    // ===== runTask =====

    @Test
    void runTaskRunsOnTheExecutorWithTheCallerTraceId() throws Exception {
        TrackingContext.setTraceId("run-task");
        CompletableFuture<String> seen = new CompletableFuture<>();
        Thread caller = Thread.currentThread();

        manager.runTask(() -> seen.complete(Thread.currentThread() != caller ? TrackingContext.currentTraceId() : "caller-thread"));

        assertThat(seen.get(2, TimeUnit.SECONDS)).isEqualTo("run-task");
    }

    @Test
    void runTaskSetsUpAndTearsDownTheExtraContextAroundTheTask() throws Exception {
        List<String> steps = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);

        manager.runTask(() -> steps.add("task"), () -> steps.add("accept"), () -> {
            steps.add("clear");
            done.countDown();
        });

        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(steps).containsExactly("accept", "task", "clear");
    }

    @Test
    void contextIsClearedEvenWhenTheTaskFails() throws Exception {
        CountDownLatch cleared = new CountDownLatch(1);

        manager.runTask(() -> {
            throw new IllegalStateException("task failed");
        }, () -> {
        }, cleared::countDown);

        assertThat(cleared.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void runTaskUsesTheGivenExecutor() throws Exception {
        CompletableFuture<String> threadName = new CompletableFuture<>();
        try (SimpleAsyncTaskExecutor other = new SimpleAsyncTaskExecutor("other-executor-")) {
            manager.runTask(() -> threadName.complete(Thread.currentThread().getName()), other);

            assertThat(threadName.get(2, TimeUnit.SECONDS)).startsWith("other-executor-");
        }
    }

    // ===== runCallable =====

    @Test
    void runCallableCompletesWithTheResultAndTheCallerTraceId() throws Exception {
        TrackingContext.setTraceId("callable");

        CompletableFuture<String> result = manager.runCallable(() -> "value:" + TrackingContext.currentTraceId());

        assertThat(result.get(2, TimeUnit.SECONDS)).isEqualTo("value:callable");
    }

    @Test
    void runCallableFailsWithTheTaskExceptionUnchanged() {
        CompletableFuture<String> result = manager.runCallable(() -> {
            throw new java.io.IOException("io failed");
        });

        assertThatThrownBy(() -> result.get(2, TimeUnit.SECONDS))
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    void runCallableUsesTheGivenExecutorAndContext() throws Exception {
        List<String> steps = new CopyOnWriteArrayList<>();
        try (SimpleAsyncTaskExecutor other = new SimpleAsyncTaskExecutor("callable-executor-")) {
            CompletableFuture<String> result = manager.runCallable(() -> {
                steps.add("task");
                return Thread.currentThread().getName();
            }, () -> steps.add("accept"), () -> steps.add("clear"), other);

            assertThat(result.get(2, TimeUnit.SECONDS)).startsWith("callable-executor-");
            assertThat(steps).containsExactly("accept", "task", "clear");
        }
    }

    // ===== zipTasks =====

    @Test
    void zipTasksRunsTheTasksInParallel() {
        long start = System.nanoTime();

        manager.zipTasks(() -> sleep(500), () -> sleep(500), () -> sleep(500), () -> sleep(500));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1500));
    }

    @Test
    void zipTasksWithoutTasksReturnsImmediately() {
        manager.zipTasks();
        manager.zipTasks((Runnable[]) null);
    }

    @Test
    void timeoutInterruptsTheTasksStillRunning() throws InterruptedException {
        ExecutorManager shortTimeout = new DefaultExecutorManager(executor, Duration.ofMillis(200));
        CountDownLatch interrupted = new CountDownLatch(1);

        assertThatThrownBy(() -> shortTimeout.zipTasks(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(10));
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
        })).isInstanceOf(ExecutorException.class).hasMessage("Task execution timeout");

        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void everyStrategyTimesOut() {
        ExecutorManager shortTimeout = new DefaultExecutorManager(executor, Duration.ofMillis(200));

        for (ExecutorManager.FailureStrategy strategy : ExecutorManager.FailureStrategy.values()) {
            long start = System.nanoTime();
            assertThatThrownBy(() -> shortTimeout.zipTasks(strategy, () -> sleep(5_000)))
                    .as(strategy.name())
                    .isInstanceOf(ExecutorException.class)
                    .hasMessage("Task execution timeout");
            assertThat(Duration.ofNanos(System.nanoTime() - start)).as(strategy.name()).isLessThan(Duration.ofSeconds(1));
        }
    }

    @Test
    void timeoutCoversTheWholeCallNotEachTask() {
        ExecutorManager shortTimeout = new DefaultExecutorManager(executor, Duration.ofMillis(400));

        // Each task finishes 150 ms after the previous one: 600 ms in all, over the 400 ms budget
        assertThatThrownBy(() -> shortTimeout.zipTasks(() -> sleep(150), () -> sleep(300), () -> sleep(450),
                () -> sleep(600)))
                .isInstanceOf(ExecutorException.class)
                .hasMessage("Task execution timeout");
    }

    @Test
    void errorCancelSucceedsWhenNothingFails() {
        List<String> done = new CopyOnWriteArrayList<>();

        manager.zipTasks(ExecutorManager.FailureStrategy.ERROR_CANCEL, () -> done.add("a"), () -> {
            sleep(200);
            done.add("b");
        });

        assertThat(done).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void callerInterruptedWhileWaitingStopsTheTasks() throws Exception {
        CountDownLatch taskInterrupted = new CountDownLatch(1);
        CompletableFuture<Throwable> callerError = new CompletableFuture<>();
        CompletableFuture<Boolean> callerFlag = new CompletableFuture<>();

        Thread caller = Thread.ofVirtual().start(() -> {
            try {
                manager.zipTasks(() -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(10));
                    } catch (InterruptedException e) {
                        taskInterrupted.countDown();
                    }
                });
            } catch (Throwable e) {
                callerError.complete(e);
                callerFlag.complete(Thread.currentThread().isInterrupted());
            }
        });
        sleep(200);
        caller.interrupt();

        assertThat(callerError.get(2, TimeUnit.SECONDS)).isInstanceOf(ExecutorException.class);
        assertThat(callerFlag.get()).isTrue();
        assertThat(taskInterrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void zipTasksWorksWhenAFullPoolRunsTasksOnTheCaller() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(1);
        pool.setQueueCapacity(0);
        pool.setTaskDecorator(RunnableWrapper::new);
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        pool.initialize();
        try {
            ExecutorManager poolManager = new DefaultExecutorManager(pool, Duration.ofSeconds(2));
            TrackingContext.setTraceId("caller");
            List<String> traces = new CopyOnWriteArrayList<>();

            poolManager.zipTasks(() -> {
                sleep(200);
                traces.add(TrackingContext.currentTraceId());
            }, () -> traces.add(TrackingContext.currentTraceId()), () -> traces.add(TrackingContext.currentTraceId()));

            assertThat(traces).containsOnly("caller").hasSize(3);
            // Tasks run inline on the caller must not take its trace id away
            assertThat(TrackingContext.currentTraceId()).isEqualTo("caller");
        } finally {
            pool.shutdown();
        }
    }

    // ===== zipTasksReturn =====

    @Test
    void zipTasksReturnSupportsThreeToSixTasksInParallel() {
        long start = System.nanoTime();

        int three = manager.zipTasksReturn(() -> slow(1), () -> slow(2), () -> slow(3), (a, b, c) -> a + b + c);
        int four = manager.zipTasksReturn(() -> slow(1), () -> slow(2), () -> slow(3), () -> slow(4),
                (a, b, c, d) -> a + b + c + d);
        int five = manager.zipTasksReturn(() -> slow(1), () -> slow(2), () -> slow(3), () -> slow(4), () -> slow(5),
                (a, b, c, d, e) -> a + b + c + d + e);
        String six = manager.zipTasksReturn(() -> "a", () -> 1, () -> 2L, () -> 3.0, () -> true, () -> 'z',
                (a, b, c, d, e, f) -> a + b + c + d + e + f);

        assertThat(three).isEqualTo(6);
        assertThat(four).isEqualTo(10);
        assertThat(five).isEqualTo(15);
        assertThat(six).isEqualTo("a123.0truez");
        // 3 rounds of 300 ms tasks; sequential would take 3.6 s
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2000));
    }

    @Test
    void zipTasksReturnPassesResultsInArgumentOrder() {
        String result = manager.zipTasksReturn(() -> {
            sleep(300);
            return "first";
        }, () -> "second", (a, b) -> a + "," + b);

        assertThat(result).isEqualTo("first,second");
    }

    @Test
    void zipTasksReturnUsesTheGivenExecutor() {
        try (SimpleAsyncTaskExecutor other = new SimpleAsyncTaskExecutor("zip-executor-")) {
            String names = manager.zipTasksReturn(other, () -> Thread.currentThread().getName(),
                    () -> Thread.currentThread().getName(), (a, b) -> a + "|" + b);

            assertThat(names.split("\\|")).allMatch(name -> name.startsWith("zip-executor-"));
        }
    }

    @Test
    void zipTasksReturnTimesOutAndInterruptsTheTasks() throws InterruptedException {
        ExecutorManager shortTimeout = new DefaultExecutorManager(executor, Duration.ofMillis(200));
        CountDownLatch interrupted = new CountDownLatch(1);
        long start = System.nanoTime();

        try (SimpleAsyncTaskExecutor other = new SimpleAsyncTaskExecutor("zip-timeout-")) {
            assertThatThrownBy(() -> shortTimeout.zipTasksReturn(other, () -> "fast", () -> {
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException e) {
                    interrupted.countDown();
                }
                return "slow";
            }, (a, b) -> a + b))
                    .isInstanceOf(ExecutorException.class)
                    .hasMessage("Task execution timeout");
        }

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void zipTasksReturnFailsFastAndInterruptsTheOthers() throws InterruptedException {
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicBoolean zipped = new AtomicBoolean();
        long start = System.nanoTime();

        assertThatThrownBy(() -> manager.zipTasksReturn(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(10));
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
            return 1;
        }, () -> {
            sleep(100);
            throw new IllegalStateException("second failed");
        }, (a, b) -> zipped.getAndSet(true)))
                .isInstanceOf(ExecutorException.class)
                .hasMessage("Task execution failed: second failed");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(zipped).isFalse();
    }

    // ===== Failures in detail =====

    @Test
    void defaultStrategyKeepsLaterFailuresAsSuppressed() {
        assertThatThrownBy(() -> manager.zipTasks(
                () -> {
                    throw new IllegalStateException("first");
                },
                () -> {
                    sleep(200);
                    throw new IllegalArgumentException("second");
                }))
                .isInstanceOf(ExecutorException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).singleElement().isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void timeoutAfterAFailureStillReportsTheFailure() {
        ExecutorManager shortTimeout = new DefaultExecutorManager(executor, Duration.ofMillis(300));

        assertThatThrownBy(() -> shortTimeout.zipTasks(
                () -> {
                    throw new IllegalStateException("failed before timeout");
                },
                () -> sleep(5_000)))
                .hasMessage("Task execution timeout")
                .satisfies(e -> assertThat(e.getSuppressed()).singleElement()
                        .satisfies(s -> assertThat(s).hasCauseInstanceOf(IllegalStateException.class)));
    }

    @Test
    void rejectedTaskStopsTheTasksAlreadySubmitted() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(1);
        pool.setQueueCapacity(0);
        pool.initialize();
        AtomicBoolean firstFinished = new AtomicBoolean();
        AtomicBoolean secondRan = new AtomicBoolean();
        try {
            ExecutorManager poolManager = new DefaultExecutorManager(pool, Duration.ofSeconds(2));

            assertThatThrownBy(() -> poolManager.zipTasks(() -> {
                sleep(10_000);
                firstFinished.set(true);
            }, () -> secondRan.set(true)))
                    .isInstanceOf(ExecutorException.class)
                    .hasMessage("Task rejected by the executor");

            // The first task is either cancelled before it starts or interrupted while it sleeps
            long deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos();
            while (pool.getActiveCount() > 0 && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertThat(pool.getActiveCount()).isZero();
            assertThat(firstFinished).isFalse();
            assertThat(secondRan).isFalse();
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void nullTaskIsRejectedBeforeAnythingRuns() {
        AtomicBoolean ran = new AtomicBoolean();

        assertThatThrownBy(() -> manager.zipTasks(() -> ran.set(true), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> manager.zipTasksReturn(() -> ran.getAndSet(true), null, (a, b) -> a))
                .isInstanceOf(NullPointerException.class);
        sleep(100);
        assertThat(ran).isFalse();
    }

    @Test
    void timeoutMustBePositive() {
        assertThatThrownBy(() -> new DefaultExecutorManager(executor, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultExecutorManager(executor, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wrappersPassTaskExceptionsThroughUnchanged() {
        IllegalStateException failure = new IllegalStateException("original");

        assertThatThrownBy(() -> new RunnableWrapper(() -> {
            throw failure;
        }).run()).isSameAs(failure);
        assertThatThrownBy(() -> new CallableWrapper<>(() -> {
            throw new java.io.IOException("checked");
        }).call()).isInstanceOf(java.io.IOException.class);
    }

    private static int slow(int value) {
        sleep(300);
        return value;
    }

    private static SimpleAsyncTaskExecutor virtualExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("executor-manager-test-");
        executor.setVirtualThreads(true);
        return executor;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

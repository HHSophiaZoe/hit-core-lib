package com.hit.spring.core.manager;

import com.hit.spring.core.exception.ExecutorException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@Component
public class ExecutorManager extends ExecutorManagerBase {

    public enum FailureStrategy {
        DEFAULT,              // Wait all task and fail to throw exception
        ERROR_CANCEL,         // Fail to throw exception and cancel other task
        IGNORE_ERROR,         // Continue despite errors
    }

    public void zipTasks(Runnable... runnables) {
        this.zipTasks(executor, FailureStrategy.DEFAULT, runnables);
    }

    public void zipTasks(FailureStrategy strategy, Runnable... runnables) {
        this.zipTasks(executor, strategy, runnables);
    }

    @SneakyThrows
    public void zipTasks(AsyncTaskExecutor executor, FailureStrategy strategy, Runnable... runnables) {
        if (runnables == null || runnables.length == 0) {
            return;
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor cannot be null");
        }

        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        AtomicReference<Exception> firstException = new AtomicReference<>();
        AtomicBoolean hasError = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(runnables.length);

        for (Runnable runnable : runnables) {
            Future<?> future;
            if (runnable instanceof WrappedRunnable) {
                future = executor.submit(runnable);
            } else {
                future = executor.submit(new WrappedRunnable(runnable) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } catch (Exception e) {
                            if (firstException.compareAndSet(null, e)) {
                                hasError.set(true);
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            futures.add(future);
        }

        switch (strategy) {
            case DEFAULT:
                if (!latch.await(taskExecutorProperties.getTaskTimeoutSeconds(), TimeUnit.SECONDS)) {
                    cleanFutureTasks(futures);
                    throw new ExecutorException("Task execution timeout");
                }
                if (hasError.get()) {
                    throw new ExecutorException("Task execution failed: " + firstException.get().getMessage(), firstException.get());
                }
                break;
            case ERROR_CANCEL:
                final long startTime = System.currentTimeMillis();
                while (latch.getCount() > 0 && System.currentTimeMillis() - startTime < taskExecutorProperties.getTaskTimeoutSeconds() * 1000L) {
                   if (hasError.get()) {
                       cleanFutureTasks(futures);
                       throw new ExecutorException("Task execution failed: " + firstException.get().getMessage(), firstException.get());
                   }
                }
                if (System.currentTimeMillis() - startTime > taskExecutorProperties.getTaskTimeoutSeconds() * 1000L) {
                    cleanFutureTasks(futures);
                    throw new ExecutorException("Task execution timeout");
                }
                if (hasError.get()) {
                    throw new ExecutorException("Task execution failed: " + firstException.get().getMessage(), firstException.get());
                }
                break;
            case IGNORE_ERROR:
                if (!latch.await(taskExecutorProperties.getTaskTimeoutSeconds(), TimeUnit.SECONDS)) {
                    cleanFutureTasks(futures);
                    throw new ExecutorException("Task execution timeout");
                }
                break;
        }
    }

    public <T1, T2, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, BiFunction<T1, T2, R> zipper) {
        return zipTasksReturn(executor, t1, t2, zipper);
    }

    @SuppressWarnings({"unchecked"})
    public <T1, T2, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, BiFunction<T1, T2, R> zipper) {
        return zipInternal(
                objects -> zipper.apply((T1) objects[0], (T2) objects[1]),
                runCompletable(t1, executor),
                runCompletable(t2, executor)
        );
    }

    public <T1, T2, T3, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Function3<T1, T2, T3, R> zipper) {
        return zipTasksReturn(executor, t1, t2, t3, zipper);
    }

    @SuppressWarnings({"unchecked"})
    public <T1, T2, T3, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Function3<T1, T2, T3, R> zipper) {
        return zipInternal(
                objects -> zipper.apply((T1) objects[0], (T2) objects[1], (T3) objects[2]),
                runCompletable(t1, executor),
                runCompletable(t2, executor),
                runCompletable(t3, executor)
        );
    }

    public <T1, T2, T3, T4, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                Function4<T1, T2, T3, T4, R> zipper) {
        return zipTasksReturn(executor, t1, t2, t3, t4, zipper);
    }

    @SuppressWarnings({"unchecked"})
    public <T1, T2, T3, T4, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3,
                                                Callable<T4> t4, Function4<T1, T2, T3, T4, R> zipper) {
        return zipInternal(
                objects -> zipper.apply((T1) objects[0], (T2) objects[1], (T3) objects[2], (T4) objects[3]),
                runCompletable(t1, executor),
                runCompletable(t2, executor),
                runCompletable(t3, executor),
                runCompletable(t4, executor)
        );
    }

    public <T1, T2, T3, T4, T5, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                    Callable<T5> t5, Function5<T1, T2, T3, T4, T5, R> zipper) {
        return zipTasksReturn(executor, t1, t2, t3, t4, t5, zipper);
    }

    @SuppressWarnings({"unchecked"})
    public <T1, T2, T3, T4, T5, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3,
                                                    Callable<T4> t4, Callable<T5> t5, Function5<T1, T2, T3, T4, T5, R> zipper) {
        return zipInternal(
                objects -> zipper.apply((T1) objects[0], (T2) objects[1], (T3) objects[2], (T4) objects[3], (T5) objects[4]),
                runCompletable(t1, executor),
                runCompletable(t2, executor),
                runCompletable(t3, executor),
                runCompletable(t4, executor),
                runCompletable(t5, executor)
        );
    }

    public <T1, T2, T3, T4, T5, T6, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                        Callable<T5> t5, Callable<T6> t6, Function6<T1, T2, T3, T4, T5, T6, R> zipper) {
        return zipTasksReturn(executor, t1, t2, t3, t4, t5, t6, zipper);
    }

    @SuppressWarnings({"unchecked"})
    public <T1, T2, T3, T4, T5, T6, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                        Callable<T5> t5, Callable<T6> t6, Function6<T1, T2, T3, T4, T5, T6, R> zipper) {
        return zipInternal(
                objects -> zipper.apply((T1) objects[0], (T2) objects[1], (T3) objects[2], (T4) objects[3], (T5) objects[4], (T6) objects[5]),
                runCompletable(t1, executor),
                runCompletable(t2, executor),
                runCompletable(t3, executor),
                runCompletable(t4, executor),
                runCompletable(t5, executor),
                runCompletable(t6, executor)
        );
    }

// internal methods
    @SneakyThrows
    protected <R> R zipInternal(Function<Object[], R> zipper, CompletableFuture<?>... futures) {
        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    Object[] results = new Object[futures.length];
                    for (int i = 0; i < futures.length; i++) {
                        results[i] = futures[i].join();
                    }
                    return zipper.apply(results);
                })
                .get(taskExecutorProperties.getTaskTimeoutSeconds(), TimeUnit.SECONDS);
    }

    protected void cleanFutureTasks(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    @FunctionalInterface
    public interface Function3<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }

    @FunctionalInterface
    public interface Function4<T1, T2, T3, T4, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4);
    }

    @FunctionalInterface
    public interface Function5<T1, T2, T3, T4, T5, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

    @FunctionalInterface
    public interface Function6<T1, T2, T3, T4, T5, T6, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
    }

}

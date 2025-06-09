package com.hit.spring.core.manager;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@Component
public class ExecutorManager extends ExecutorBaseManager {

    public enum FailureStrategy {
        DEFAULT,              // Fail to throw exception
        IGNORE_ERRORS,        // Continue despite errors
    }

    public void zipTasks(FailureStrategy strategy, Runnable... runnables) {
        this.zipTasks(executor, strategy, runnables);
    }

    public void zipTasks(AsyncTaskExecutor executor, FailureStrategy strategy, Runnable... runnables) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Runnable runnable : runnables) {
            CompletableFuture<Void> future;
            switch (strategy) {
                case DEFAULT:
                    future = this.runCompletable(runnable, executor);
                    break;
                case IGNORE_ERRORS:
                    future = this.runCompletable(runnable, executor)
                            .exceptionally(ex -> {
                                log.error("runCompletable {} error: {}", runnable, ex.getMessage(), ex);
                                return null;
                            });
                    break;
                default:
                    throw new IllegalArgumentException("Unknown strategy: " + strategy);
            }
            futures.add(future);
        }
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();
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
    private <R> R zipInternal(Function<Object[], R> zipper, CompletableFuture<?>... futures) {
        return CompletableFuture
                .allOf(futures)
                .thenApply(v -> {
                    Object[] results = new Object[futures.length];
                    for (int i = 0; i < futures.length; i++) {
                        results[i] = futures[i].join();
                    }
                    return zipper.apply(results);
                })
                .join();
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

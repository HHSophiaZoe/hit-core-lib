package com.hit.spring.task;

import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Runs application tasks asynchronously or in parallel, keeping the trace id of the caller. The overloads with an
 * {@link AsyncTaskExecutor} use that executor instead of the application one; {@code acceptContext}/{@code clearContext}
 * set up and tear down extra thread context around the task.
 */
public interface ExecutorManager {

    /**
     * What {@code zipTasks} does when a task fails.
     */
    enum FailureStrategy {
        /** Waits for every task, then throws the first failure (later ones are suppressed). */
        DEFAULT,
        /** Throws on the first failure and interrupts the tasks still running. */
        ERROR_CANCEL,
        /** Logs failures and returns normally. */
        IGNORE_ERROR,
    }

    /**
     * Runs the task on the application executor without waiting.
     */
    void runTask(Runnable runnable);

    void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext);

    void runTask(Runnable runnable, AsyncTaskExecutor executor);

    void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor);

    /**
     * Runs the task on the application executor; the future completes with its result or its exception unchanged.
     * Cancelling the future does not interrupt the task.
     */
    <T> CompletableFuture<T> runCallable(Callable<T> callable);

    <T> CompletableFuture<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext);

    <T> CompletableFuture<T> runCallable(Callable<T> callable, AsyncTaskExecutor executor);

    <T> CompletableFuture<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor);

    /**
     * Runs the tasks in parallel and waits for all of them with {@link FailureStrategy#DEFAULT}.
     */
    void zipTasks(Runnable... runnables);

    void zipTasks(FailureStrategy strategy, Runnable... runnables);

    /**
     * Runs the tasks in parallel and waits up to {@code app.task.executor.task-timeout-seconds}. When the call throws,
     * the tasks still running are interrupted.
     *
     * @throws com.hit.spring.task.ExecutorException on timeout, interrupt, rejection by the executor or,
     *                                                         unless {@link FailureStrategy#IGNORE_ERROR}, on task
     *                                                         failure (the task exception is the cause)
     */
    void zipTasks(AsyncTaskExecutor executor, FailureStrategy strategy, Runnable... runnables);

    /**
     * Runs the tasks in parallel and combines their results, in argument order. Same timeout as {@code zipTasks};
     * fails on the first task failure ({@link FailureStrategy#ERROR_CANCEL}) since the results are then unusable.
     *
     * @throws com.hit.spring.task.ExecutorException on timeout, interrupt, rejection or task failure
     */
    <T1, T2, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, BiFunction<T1, T2, R> zipper);

    <T1, T2, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, BiFunction<T1, T2, R> zipper);

    <T1, T2, T3, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Function3<T1, T2, T3, R> zipper);

    <T1, T2, T3, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Function3<T1, T2, T3, R> zipper);

    <T1, T2, T3, T4, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                         Function4<T1, T2, T3, T4, R> zipper);

    <T1, T2, T3, T4, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3,
                                         Callable<T4> t4, Function4<T1, T2, T3, T4, R> zipper);

    <T1, T2, T3, T4, T5, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                             Callable<T5> t5, Function5<T1, T2, T3, T4, T5, R> zipper);

    <T1, T2, T3, T4, T5, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3,
                                             Callable<T4> t4, Callable<T5> t5, Function5<T1, T2, T3, T4, T5, R> zipper);

    <T1, T2, T3, T4, T5, T6, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                 Callable<T5> t5, Callable<T6> t6, Function6<T1, T2, T3, T4, T5, T6, R> zipper);

    <T1, T2, T3, T4, T5, T6, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                 Callable<T5> t5, Callable<T6> t6, Function6<T1, T2, T3, T4, T5, T6, R> zipper);

    @FunctionalInterface
    interface Function3<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }

    @FunctionalInterface
    interface Function4<T1, T2, T3, T4, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4);
    }

    @FunctionalInterface
    interface Function5<T1, T2, T3, T4, T5, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);
    }

    @FunctionalInterface
    interface Function6<T1, T2, T3, T4, T5, T6, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
    }
}

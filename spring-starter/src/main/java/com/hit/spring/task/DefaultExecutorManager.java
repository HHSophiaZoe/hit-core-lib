package com.hit.spring.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * {@link ExecutorManager} on an {@link AsyncTaskExecutor}, by default the application executor
 * ({@code appTaskExecutor}). Tasks are wrapped so they run with the trace id of the caller.
 */
@Slf4j
public class DefaultExecutorManager implements ExecutorManager {

    private final AsyncTaskExecutor executor;

    private final Duration taskTimeout;

    public DefaultExecutorManager(AsyncTaskExecutor executor, Duration taskTimeout) {
        this.executor = Objects.requireNonNull(executor, "Executor cannot be null");
        if (taskTimeout == null || !taskTimeout.isPositive()) {
            throw new IllegalArgumentException("Task timeout must be positive: " + taskTimeout);
        }
        this.taskTimeout = taskTimeout;
    }

    // ===== Fire and forget =====

    @Override
    public void runTask(Runnable runnable) {
        this.runTask(runnable, null, null, executor);
    }

    @Override
    public void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext) {
        this.runTask(runnable, acceptContext, clearContext, executor);
    }

    @Override
    public void runTask(Runnable runnable, AsyncTaskExecutor executor) {
        this.runTask(runnable, null, null, executor);
    }

    @Override
    public void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor) {
        executor.execute(new RunnableWrapper(runnable, acceptContext, clearContext));
    }

    @Override
    public <T> CompletableFuture<T> runCallable(Callable<T> callable) {
        return this.runCallable(callable, null, null, executor);
    }

    @Override
    public <T> CompletableFuture<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext) {
        return this.runCallable(callable, acceptContext, clearContext, executor);
    }

    @Override
    public <T> CompletableFuture<T> runCallable(Callable<T> callable, AsyncTaskExecutor executor) {
        return this.runCallable(callable, null, null, executor);
    }

    @Override
    public <T> CompletableFuture<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext,
                                                AsyncTaskExecutor executor) {
        return executor.submitCompletable(new CallableWrapper<>(callable, acceptContext, clearContext));
    }

    // ===== Run in parallel and wait =====

    @Override
    public void zipTasks(Runnable... runnables) {
        this.zipTasks(executor, FailureStrategy.DEFAULT, runnables);
    }

    @Override
    public void zipTasks(FailureStrategy strategy, Runnable... runnables) {
        this.zipTasks(executor, strategy, runnables);
    }

    @Override
    public void zipTasks(AsyncTaskExecutor executor, FailureStrategy strategy, Runnable... runnables) {
        if (runnables == null || runnables.length == 0) {
            return;
        }
        Callable<?>[] tasks = new Callable<?>[runnables.length];
        for (int i = 0; i < runnables.length; i++) {
            tasks[i] = Executors.callable(Objects.requireNonNull(runnables[i], "Task cannot be null"));
        }
        this.invokeAll(executor, Objects.requireNonNull(strategy, "Strategy cannot be null"), tasks);
    }

    @Override
    public <T1, T2, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, BiFunction<T1, T2, R> zipper) {
        return this.zipTasksReturn(executor, t1, t2, zipper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1, T2, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2,
                                        BiFunction<T1, T2, R> zipper) {
        Object[] r = this.invokeAll(executor, FailureStrategy.ERROR_CANCEL, t1, t2);
        return zipper.apply((T1) r[0], (T2) r[1]);
    }

    @Override
    public <T1, T2, T3, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3,
                                            Function3<T1, T2, T3, R> zipper) {
        return this.zipTasksReturn(executor, t1, t2, t3, zipper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2,
                                            Callable<T3> t3, Function3<T1, T2, T3, R> zipper) {
        Object[] r = this.invokeAll(executor, FailureStrategy.ERROR_CANCEL, t1, t2, t3);
        return zipper.apply((T1) r[0], (T2) r[1], (T3) r[2]);
    }

    @Override
    public <T1, T2, T3, T4, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                Function4<T1, T2, T3, T4, R> zipper) {
        return this.zipTasksReturn(executor, t1, t2, t3, t4, zipper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2,
                                                Callable<T3> t3, Callable<T4> t4, Function4<T1, T2, T3, T4, R> zipper) {
        Object[] r = this.invokeAll(executor, FailureStrategy.ERROR_CANCEL, t1, t2, t3, t4);
        return zipper.apply((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3]);
    }

    @Override
    public <T1, T2, T3, T4, T5, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                    Callable<T5> t5, Function5<T1, T2, T3, T4, T5, R> zipper) {
        return this.zipTasksReturn(executor, t1, t2, t3, t4, t5, zipper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2,
                                                    Callable<T3> t3, Callable<T4> t4, Callable<T5> t5,
                                                    Function5<T1, T2, T3, T4, T5, R> zipper) {
        Object[] r = this.invokeAll(executor, FailureStrategy.ERROR_CANCEL, t1, t2, t3, t4, t5);
        return zipper.apply((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3], (T5) r[4]);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, R> R zipTasksReturn(Callable<T1> t1, Callable<T2> t2, Callable<T3> t3, Callable<T4> t4,
                                                        Callable<T5> t5, Callable<T6> t6,
                                                        Function6<T1, T2, T3, T4, T5, T6, R> zipper) {
        return this.zipTasksReturn(executor, t1, t2, t3, t4, t5, t6, zipper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1, T2, T3, T4, T5, T6, R> R zipTasksReturn(AsyncTaskExecutor executor, Callable<T1> t1, Callable<T2> t2,
                                                        Callable<T3> t3, Callable<T4> t4, Callable<T5> t5,
                                                        Callable<T6> t6, Function6<T1, T2, T3, T4, T5, T6, R> zipper) {
        Object[] r = this.invokeAll(executor, FailureStrategy.ERROR_CANCEL, t1, t2, t3, t4, t5, t6);
        return zipper.apply((T1) r[0], (T2) r[1], (T3) r[2], (T4) r[3], (T5) r[4], (T6) r[5]);
    }

    /**
     * Runs the tasks in parallel and returns their results in argument order. Whatever way it leaves (timeout,
     * failure with {@link FailureStrategy#ERROR_CANCEL}, interrupt, rejection), no task is left running.
     */
    private Object[] invokeAll(AsyncTaskExecutor executor, FailureStrategy strategy, Callable<?>... tasks) {
        Objects.requireNonNull(executor, "Executor cannot be null");
        for (Callable<?> task : tasks) {
            Objects.requireNonNull(task, "Task cannot be null");
        }
        Object[] results = new Object[tasks.length];
        // Completion order lets ERROR_CANCEL react to the first failure, not the first task in the list
        CompletionService<Object> completion = new ExecutorCompletionService<>(executor);
        List<Future<Object>> futures = new ArrayList<>(tasks.length);
        try {
            for (int i = 0; i < tasks.length; i++) {
                int index = i;
                Callable<?> task = tasks[i];
                futures.add(completion.submit(new CallableWrapper<>(() -> results[index] = task.call())));
            }
            this.await(completion, tasks.length, strategy);
            // The completion queue hands each finished task over to this thread, so its result is visible
            return results;
        } catch (RejectedExecutionException e) {
            throw new ExecutorException("Task rejected by the executor", e);
        } finally {
            // Only tasks still running are interrupted; on success every future is already done
            futures.forEach(future -> future.cancel(true));
        }
    }

    private void await(CompletionService<Object> completion, int count, FailureStrategy strategy) {
        long deadline = System.nanoTime() + taskTimeout.toNanos();
        ExecutorException failure = null;
        try {
            for (int i = 0; i < count; i++) {
                Future<Object> done = completion.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (done == null) {
                    ExecutorException timeout = new ExecutorException("Task execution timeout");
                    if (failure != null) {
                        timeout.addSuppressed(failure);
                    }
                    throw timeout;
                }
                if (done.state() != Future.State.FAILED) {
                    continue;
                }
                Throwable error = done.exceptionNow();
                switch (strategy) {
                    case IGNORE_ERROR -> log.warn("Ignored task failure: {}", error.getMessage(), error);
                    case ERROR_CANCEL -> throw failed(error);
                    case DEFAULT -> {
                        if (failure == null) {
                            failure = failed(error);
                        } else {
                            failure.addSuppressed(error);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutorException("Interrupted while waiting for tasks", e);
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static ExecutorException failed(Throwable error) {
        return new ExecutorException("Task execution failed: " + error.getMessage(), error);
    }
}

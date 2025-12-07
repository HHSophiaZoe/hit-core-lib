package com.hit.spring.core.manager;

import com.hit.spring.config.properties.TaskExecutorProperties;
import com.hit.spring.core.wrapper.CallableWrapper;
import com.hit.spring.core.wrapper.RunnableWrapper;
import com.hit.spring.core.extension.Procedure;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public abstract class ExecutorManagerBase {

    @Setter(onMethod_ = {@Autowired(required = false), @Qualifier("appTaskExecutor")})
    protected AsyncTaskExecutor executor;

    @Setter(onMethod_ = {@Autowired})
    protected TaskExecutorProperties taskExecutorProperties;

    //Runnable
    public void runTask(Runnable runnable) {
        executor.execute(new RunnableWrapper(runnable));
    }

    public void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext) {
        executor.execute(new RunnableWrapper(runnable, acceptContext, clearContext));
    }

    public void runTask(Runnable runnable, AsyncTaskExecutor executor) {
        executor.execute(new RunnableWrapper(runnable));
    }

    public void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor) {
        executor.execute(new RunnableWrapper(runnable, acceptContext, clearContext));
    }

    //Callable
    public <T> CompletableFuture<T> runCallable(Callable<T> callable) {
        return executor.submitCompletable(new CallableWrapper<>(callable));
    }

    public <T> CompletableFuture<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext) {
        return executor.submitCompletable(new CallableWrapper<>(callable, acceptContext, clearContext));
    }

    public <T> CompletableFuture<T> runCallable(Callable<T> callable, AsyncTaskExecutor executor) {
        return executor.submitCompletable(new CallableWrapper<>(callable));
    }

    public <T> CompletableFuture<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor) {
        return executor.submitCompletable(new CallableWrapper<>(callable, acceptContext, clearContext));
    }

    protected <T> CompletableFuture<T> runCallableInternal(Callable<T> callable, AsyncTaskExecutor executor) {
        Objects.requireNonNull(executor, "Executor cannot be null");
        return CompletableFuture.supplyAsync(() -> {
            if (callable instanceof CallableWrapper) {
                return ((CallableWrapper<T>) callable).call();
            } else {
                CallableWrapper<T> callableWrapper = new CallableWrapper<>(callable);
                return callableWrapper.call();
            }
        }, executor);
    }

}

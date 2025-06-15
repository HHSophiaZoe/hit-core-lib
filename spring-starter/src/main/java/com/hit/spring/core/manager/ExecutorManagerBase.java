package com.hit.spring.core.manager;

import com.hit.spring.config.properties.TaskExecutorProperties;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;
import lombok.Setter;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static com.hit.spring.core.constants.enums.TrackingContextEnum.CORRELATION_ID;

public abstract class ExecutorManagerBase {

    @Setter(onMethod_ = {@Autowired(required = false), @Qualifier("appTaskExecutor")})
    protected AsyncTaskExecutor executor;

    @Setter(onMethod_ = {@Autowired})
    protected TaskExecutorProperties taskExecutorProperties;

    //Runnable
    public void runTask(Runnable runnable) {
        executor.execute(new WrappedRunnable(runnable));
    }

    public void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext) {
        executor.execute(new WrappedRunnable(runnable, acceptContext, clearContext));
    }

    public void runTask(Runnable runnable, AsyncTaskExecutor executor) {
        executor.execute(new WrappedRunnable(runnable));
    }

    public void runTask(Runnable runnable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor) {
        executor.execute(new WrappedRunnable(runnable, acceptContext, clearContext));
    }

    //Callable
    public <T> Future<T> runCallable(Callable<T> callable) {
        return executor.submit(new WrappedCallable<>(callable));
    }

    public <T> Future<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext) {
        return executor.submit(new WrappedCallable<>(callable, acceptContext, clearContext));
    }

    public <T> Future<T> runCallable(Callable<T> callable, AsyncTaskExecutor executor) {
        return executor.submit(new WrappedCallable<>(callable));
    }

    public <T> Future<T> runCallable(Callable<T> callable, Procedure acceptContext, Procedure clearContext, AsyncTaskExecutor executor) {
        return executor.submit(new WrappedCallable<>(callable, acceptContext, clearContext));
    }

    // internal method
    protected <T> CompletableFuture<T> runCompletable(Callable<T> callable, AsyncTaskExecutor executor) {
        Objects.requireNonNull(executor, "Executor cannot be null");
        return CompletableFuture.supplyAsync(() -> {
            if (callable instanceof WrappedCallable) {
                return ((WrappedCallable<T>) callable).call();
            } else {
                WrappedCallable<T> wrappedCallable = new WrappedCallable<>(callable);
                return wrappedCallable.call();
            }
        }, executor);
    }

}

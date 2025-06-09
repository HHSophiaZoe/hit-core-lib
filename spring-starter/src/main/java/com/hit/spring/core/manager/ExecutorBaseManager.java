package com.hit.spring.core.manager;

import com.hit.spring.core.exception.BusinessException;
import com.hit.spring.core.extension.Procedure;
import lombok.Setter;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.hit.spring.core.constants.enums.TrackingContextEnum.CORRELATION_ID;

public abstract class ExecutorBaseManager {

    @Setter(onMethod_ = {@Autowired(required = false), @Qualifier("appTaskExecutor")})
    protected AsyncTaskExecutor executor;

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
    protected CompletableFuture<Void> runCompletable(Runnable runnable, AsyncTaskExecutor executor) {
        return CompletableFuture.runAsync(new WrappedRunnable(runnable), executor);
    }

    protected <T> CompletableFuture<T> runCompletable(Callable<T> callable, AsyncTaskExecutor executor) {
        return CompletableFuture.supplyAsync(() -> {
            WrappedCallable<T> wrappedCallable = new WrappedCallable<>(callable);
            return wrappedCallable.call();
        }, executor);
    }

    protected static class WrappedCallable<T> implements Callable<T> {

        private final Callable<T> task;
        private final String correlationId;

        private Procedure acceptContext;
        private Procedure clearContext;

        private WrappedCallable(Callable<T> task) {
            this.task = task;
            this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
        }

        private WrappedCallable(Callable<T> task, Procedure acceptContext, Procedure clearContext) {
            this.task = task;
            this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
            this.acceptContext = acceptContext;
            this.clearContext = clearContext;
        }

        @Override
        public T call() {
            try {
                ThreadContext.put(CORRELATION_ID.getKey(), correlationId);
                if (acceptContext != null) {
                    acceptContext.process();
                }
                return task.call();
            } catch (Exception e) {
                throw new BusinessException(e.getMessage(), e);
            } finally {
                ThreadContext.clearAll();
                if (clearContext != null) {
                    clearContext.process();
                }
            }
        }
    }

    protected static class WrappedRunnable implements Runnable {

        private final Runnable task;
        private final String correlationId;

        private Procedure acceptContext;
        private Procedure clearContext;

        private WrappedRunnable(Runnable task) {
            this.task = task;
            this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
        }

        private WrappedRunnable(Runnable task, Procedure acceptContext, Procedure clearContext) {
            this.task = task;
            this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
            this.acceptContext = acceptContext;
            this.clearContext = clearContext;
        }

        @Override
        public void run() {
            try {
                ThreadContext.put(CORRELATION_ID.getKey(), correlationId);
                if (acceptContext != null) {
                    acceptContext.process();
                }
                task.run();
            } catch (Exception e) {
                throw new BusinessException(e.getMessage(), e);
            } finally {
                ThreadContext.clearAll();
                if (clearContext != null) {
                    clearContext.process();
                }
            }
        }
    }

}

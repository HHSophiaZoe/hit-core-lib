package com.hit.spring.core.manager;

import com.hit.spring.context.TrackingContext;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.Callable;

public class WrappedCallable<T> implements Callable<T> {

    private final Callable<T> task;
    private final String correlationId;

    private Procedure acceptContext;
    private Procedure clearContext;

    protected WrappedCallable(Callable<T> task) {
        this.task = task;
        this.correlationId = TrackingContext.getCorrelationId();
    }

    protected WrappedCallable(Callable<T> task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.correlationId = TrackingContext.getCorrelationId();
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public T call() {
        try {
            TrackingContext.setCorrelationId(correlationId);
            TrackingContext.setThreadId();
            if (acceptContext != null) {
                acceptContext.process();
            }
            return task.call();
        } catch (Exception e) {
            throw new ExecutorException(e.getMessage(), e);
        } finally {
            ThreadContext.clearAll();
            if (clearContext != null) {
                clearContext.process();
            }
        }
    }
}
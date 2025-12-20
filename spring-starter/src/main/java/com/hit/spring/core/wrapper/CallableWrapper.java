package com.hit.spring.core.wrapper;

import com.hit.spring.context.TrackingContext;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;

import java.util.concurrent.Callable;

public class CallableWrapper<T> implements Callable<T> {

    private final Callable<T> task;
    private final String correlationId;

    private Procedure acceptContext;
    private Procedure clearContext;

    public CallableWrapper(Callable<T> task) {
        this.task = task;
        this.correlationId = TrackingContext.getCorrelationId();
    }

    public CallableWrapper(Callable<T> task, Procedure acceptContext, Procedure clearContext) {
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
            TrackingContext.clearContext();
            if (clearContext != null) {
                clearContext.process();
            }
        }
    }
}
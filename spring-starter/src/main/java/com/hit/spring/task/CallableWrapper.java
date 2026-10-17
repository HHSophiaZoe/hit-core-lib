package com.hit.spring.task;

import com.hit.spring.tracing.TrackingContext;

import java.util.concurrent.Callable;

/**
 * Calls a task with the trace id of the thread that created the wrapper, then puts the worker's own trace id back.
 * Exceptions of the task pass through unchanged.
 */
public class CallableWrapper<T> implements Callable<T> {

    private final Callable<T> task;

    private final String traceId;

    private final Procedure acceptContext;

    private final Procedure clearContext;

    public CallableWrapper(Callable<T> task) {
        this(task, null, null);
    }

    public CallableWrapper(Callable<T> task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.traceId = TrackingContext.getTraceId();
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public T call() throws Exception {
        String previousTraceId = TrackingContext.currentTraceId();
        try {
            TrackingContext.setTraceId(traceId);
            if (acceptContext != null) {
                acceptContext.process();
            }
            return task.call();
        } finally {
            if (clearContext != null) {
                clearContext.process();
            }
            TrackingContext.restoreTraceId(previousTraceId);
        }
    }
}

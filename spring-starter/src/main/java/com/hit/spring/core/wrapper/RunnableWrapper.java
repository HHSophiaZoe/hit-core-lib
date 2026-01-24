package com.hit.spring.core.wrapper;

import com.hit.spring.context.TrackingContext;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;

public class RunnableWrapper implements Runnable {

    private final Runnable task;
    private final String traceId;

    private Procedure acceptContext;
    private Procedure clearContext;

    public RunnableWrapper(Runnable task) {
        this.task = task;
        this.traceId = TrackingContext.getTraceId();
    }

    public RunnableWrapper(Runnable task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.traceId = TrackingContext.getTraceId();
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public void run() {
        try {
            TrackingContext.setTraceId(traceId);
            if (acceptContext != null) {
                acceptContext.process();
            }
            task.run();
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

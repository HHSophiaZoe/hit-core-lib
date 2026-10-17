package com.hit.spring.task;

import com.hit.spring.tracing.TrackingContext;

/**
 * Runs a task with the trace id of the thread that created the wrapper, then puts the worker's own trace id back.
 * Exceptions of the task pass through unchanged.
 */
public class RunnableWrapper implements Runnable {

    private final Runnable task;

    private final String traceId;

    private final Procedure acceptContext;

    private final Procedure clearContext;

    public RunnableWrapper(Runnable task) {
        this(task, null, null);
    }

    public RunnableWrapper(Runnable task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.traceId = TrackingContext.getTraceId();
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public void run() {
        String previousTraceId = TrackingContext.currentTraceId();
        try {
            TrackingContext.setTraceId(traceId);
            if (acceptContext != null) {
                acceptContext.process();
            }
            task.run();
        } finally {
            if (clearContext != null) {
                clearContext.process();
            }
            TrackingContext.restoreTraceId(previousTraceId);
        }
    }
}

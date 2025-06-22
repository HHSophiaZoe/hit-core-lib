package com.hit.spring.core.manager;

import com.hit.spring.context.TrackingContext;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;
import org.apache.logging.log4j.ThreadContext;

public class WrappedRunnable implements Runnable {

    private final Runnable task;
    private final String correlationId;

    private Procedure acceptContext;
    private Procedure clearContext;

    protected WrappedRunnable(Runnable task) {
        this.task = task;
        this.correlationId = TrackingContext.getCorrelationId();
    }

    protected WrappedRunnable(Runnable task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.correlationId = TrackingContext.getCorrelationId();
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public void run() {
        try {
            TrackingContext.setCorrelationId(correlationId);
            TrackingContext.setThreadId();
            if (acceptContext != null) {
                acceptContext.process();
            }
            task.run();
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

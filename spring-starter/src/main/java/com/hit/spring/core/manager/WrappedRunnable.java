package com.hit.spring.core.manager;

import com.hit.spring.core.constants.enums.TrackingContextEnum;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;
import org.apache.logging.log4j.ThreadContext;

import static com.hit.spring.core.constants.enums.TrackingContextEnum.CORRELATION_ID;
import static com.hit.spring.core.constants.enums.TrackingContextEnum.THREAD_ID;

public class WrappedRunnable implements Runnable {

    private final Runnable task;
    private final String correlationId;

    private Procedure acceptContext;
    private Procedure clearContext;

    protected WrappedRunnable(Runnable task) {
        this.task = task;
        this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
    }

    protected WrappedRunnable(Runnable task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public void run() {
        try {
            ThreadContext.put(CORRELATION_ID.getKey(), correlationId);
            ThreadContext.put(THREAD_ID.getKey(), TrackingContextEnum.genThreadId());
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

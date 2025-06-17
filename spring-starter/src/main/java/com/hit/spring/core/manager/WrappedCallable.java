package com.hit.spring.core.manager;

import com.hit.spring.core.constant.enums.TrackingContextEnum;
import com.hit.spring.core.exception.ExecutorException;
import com.hit.spring.core.extension.Procedure;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.Callable;

import static com.hit.spring.core.constant.enums.TrackingContextEnum.CORRELATION_ID;
import static com.hit.spring.core.constant.enums.TrackingContextEnum.THREAD_ID;

public class WrappedCallable<T> implements Callable<T> {

    private final Callable<T> task;
    private final String correlationId;

    private Procedure acceptContext;
    private Procedure clearContext;

    protected WrappedCallable(Callable<T> task) {
        this.task = task;
        this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
    }

    protected WrappedCallable(Callable<T> task, Procedure acceptContext, Procedure clearContext) {
        this.task = task;
        this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
        this.acceptContext = acceptContext;
        this.clearContext = clearContext;
    }

    @Override
    public T call() {
        try {
            ThreadContext.put(CORRELATION_ID.getKey(), correlationId);
            ThreadContext.put(THREAD_ID.getKey(), TrackingContextEnum.genThreadId());
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
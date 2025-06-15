package com.hit.spring.core.exception;

public class ExecutorException extends RuntimeException {

    public ExecutorException(Throwable cause) {
        super(cause);
    }

    public ExecutorException(String message) {
        super(message);
    }

    public ExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

}

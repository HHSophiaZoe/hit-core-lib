package com.hit.spring.core.exception;

public class HttpResponseInvalidException extends RuntimeException {

    public HttpResponseInvalidException(String message) {
        super(message);
    }

    public HttpResponseInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

}

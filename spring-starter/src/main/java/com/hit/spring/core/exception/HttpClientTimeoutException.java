package com.hit.spring.core.exception;

public class HttpClientTimeoutException extends RuntimeException {

    public HttpClientTimeoutException(String message) {
        super(message);
    }

    public HttpClientTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}

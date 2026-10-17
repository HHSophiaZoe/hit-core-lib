package com.hit.spring.http;

public class HttpResponseInvalidException extends RuntimeException {

    public HttpResponseInvalidException(String message) {
        super(message);
    }

    public HttpResponseInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

}

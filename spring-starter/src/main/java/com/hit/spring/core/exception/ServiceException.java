package com.hit.spring.core.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final String code;
    private final Object message;

    public ServiceException(String code, Object message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message != null ? message.toString() : null;
    }
}

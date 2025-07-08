package com.hit.spring.core.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreamingException extends RuntimeException {

    public StreamingException(String message, Throwable cause) {
        super(message, cause);
    }

}

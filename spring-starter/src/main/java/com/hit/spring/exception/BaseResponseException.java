package com.hit.spring.exception;

import com.hit.common.model.ResponseStatus;
import com.hit.common.model.ResponseStatusCode;
import lombok.Getter;

/**
 * Error answered to the client either with a status code (message translated from the code and params) or with a
 * ready-made {@link ResponseStatus} (HTTP 400).
 */
@Getter
public class BaseResponseException extends RuntimeException {

    private final transient ResponseStatusCode responseStatusCode;

    private final ResponseStatus responseStatus;

    private final String[] params;

    public BaseResponseException(ResponseStatusCode responseStatusCode) {
        this(responseStatusCode, null);
    }

    public BaseResponseException(ResponseStatusCode responseStatusCode, String[] params) {
        super(responseStatusCode.code());
        this.responseStatusCode = responseStatusCode;
        this.responseStatus = null;
        this.params = params;
    }

    public BaseResponseException(ResponseStatus responseStatus) {
        super(responseStatus.getCode());
        this.responseStatus = responseStatus;
        this.responseStatusCode = null;
        this.params = null;
    }
}

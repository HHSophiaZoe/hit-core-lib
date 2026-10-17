package com.hit.spring.http;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Data
@Accessors(chain = true)
public class InternalResponse<T> {

    private HttpStatus httpStatus;

    private T response;

}

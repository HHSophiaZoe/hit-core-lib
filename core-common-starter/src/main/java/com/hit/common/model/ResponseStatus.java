package com.hit.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResponseStatus {

    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object message;

}

package com.hit.spring.core.data.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hit.spring.config.locale.Translator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ResponseStatus {

    private String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object message;

    public ResponseStatus(String code, Object message) {
        this.code = code;
        this.message = message;
    }

    public ResponseStatus(String code, String[] params, boolean setMessageImplicitly) {
        this.setStatus(code, params, setMessageImplicitly);
    }

    public void setStatus(String code, String[] params, boolean setMessageImplicitly) {
        this.code = code;
        if (setMessageImplicitly) {
            this.message = Translator.toLocale(code, params);
        }
    }
}

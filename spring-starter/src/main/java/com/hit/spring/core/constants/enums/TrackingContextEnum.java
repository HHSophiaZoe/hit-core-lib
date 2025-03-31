package com.hit.spring.core.constants.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TrackingContextEnum {

    CORRELATION_ID("correlation_id");

    private final String key;

}

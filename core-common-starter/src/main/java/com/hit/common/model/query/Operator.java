package com.hit.common.model.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@Getter
@AllArgsConstructor
public enum Operator {

    EQUAL("eq"),
    NOT_EQUAL("ne"),
    IN("in"),
    NIN("nin"),
    LIKE("like"),
    GREATER_THAN("gt"),
    LESS_THAN("lt"),
    GREATER_THAN_OR_EQUAL("gte"),
    LESS_THAN_OR_EQUAL("lte"),
    NULL("null"),
    NOT_NULL("notnull"),
    NONE("none");

    private final String value;

    private static final Map<String, Operator> MAPPING_OPERATOR = new HashMap<>();

    static {
        for (Operator operator : Operator.values()) {
            MAPPING_OPERATOR.put(operator.getValue(), operator);
        }
    }

    public static Operator fromOperator(String operator) {
        return MAPPING_OPERATOR.getOrDefault(operator, NONE);
    }

    public static Set<String> operatorFilterListValue() {
        return Set.of(Operator.IN.getValue(), Operator.NIN.getValue());
    }

}

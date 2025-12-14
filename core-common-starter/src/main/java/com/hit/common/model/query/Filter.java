package com.hit.common.model.query;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Filter {

    @Parameter(description = "Filter operator:  IN: in, NIN: nin, EQUAL: eq, LIKE: like, NOT_EQUAL: ne, GREATER_THAN:" +
            " gt, LESS_THAN: lt, GREATER_THAN_OR_EQUAL: gte, LESS_THAN_OR_EQUAL: lte, NULL: null, NOT_NULL: notnull")
    private String operator = Operator.LIKE.getValue();

    @Parameter(description = "The name of property want to filter.")
    private String name;

    @Parameter(description = "Filter value")
    private String value;

    public Filter(String operator, String name) {
        this.operator = operator;
        this.name = name;
    }
}

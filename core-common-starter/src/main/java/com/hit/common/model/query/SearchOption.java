package com.hit.common.model.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum SearchOption {

    EQUAL("eq"),
    LIKE("lk"),
    LIKE_REGEX("lk_rgx"),
    LIKE_IGNORE_CASE("lk_igc"),
    LIKE_IGNORE_ACCENT("lk_iga"),
    LIKE_IGNORE_CASE_AND_ACCENT("lk_igca");

    private final String value;

    private static final Map<String, SearchOption> MAPPING_OPTION = new HashMap<>();

    static {
        for (SearchOption option : SearchOption.values()) {
            MAPPING_OPTION.put(option.getValue(), option);
        }
    }

    public static SearchOption fromOption(String operator) {
        return MAPPING_OPTION.getOrDefault(operator, null);
    }

}

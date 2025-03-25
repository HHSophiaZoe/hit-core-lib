package com.hit.coremodel.query;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Search {

    @Parameter(description = "The name of property want to search.")
    private String name;

    @Parameter(description = "Search option: EQUAL: eq, LIKE: lk, LIKE_REGEX: lk_rgx, LIKE_IGNORE_CASE: lk_igc, LIKE_IGNORE_ACCENT: lk_iga, LIKE_IGNORE_CASE_AND_ACCENT: lk_igca")
    private String option = SearchOption.LIKE.getValue();

}

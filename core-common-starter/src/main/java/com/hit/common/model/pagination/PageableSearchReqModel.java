package com.hit.common.model.pagination;

import com.hit.common.model.query.Search;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PageableSearchReqModel extends PageableReqModel {

    @Parameter(description = "List of property to search")
    private List<Search> searches;

    @Parameter(description = "Keyword to search.")
    private String keyword;

    public String getKeyword() {
        return keyword != null ? keyword.trim() : null;
    }

}

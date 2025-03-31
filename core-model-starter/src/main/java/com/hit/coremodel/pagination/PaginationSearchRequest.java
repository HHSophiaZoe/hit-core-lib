package com.hit.coremodel.pagination;

import com.hit.coremodel.query.Search;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationSearchRequest extends PaginationRequest {

    @Parameter(description = "List of property to search")
    private List<Search> searches;

    @Parameter(description = "Keyword to search.")
    private String keyword;

    public String getKeyword() {
        return keyword != null ? keyword.trim() : null;
    }

}

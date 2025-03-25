package com.hit.coremodel.pagination;

import com.hit.coremodel.query.Filter;
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
public class PaginationRequest {

    public static final Integer PAGE_NUM_DEFAULT = 1;
    public static final Integer PAGE_SIZE_DEFAULT = 10;

    @Parameter(description = "Page you want to retrieve (1..N)")
    protected Integer page;

    @Parameter(description = "Number of records per page.")
    protected Integer pageSize;

    @Parameter(description = "List of property want to sort.")
    protected List<Order> sorts;

    @Parameter(description = "List of property to filter")
    private List<Filter> filters;

    public int getPage() {
        if (page == null || page < 1) {
            page = PAGE_NUM_DEFAULT;
        }
        return page - 1;
    }

    public int getPageSize() {
        if (pageSize == null || pageSize < 1) {
            pageSize = PAGE_SIZE_DEFAULT;
        }
        return pageSize;
    }

}

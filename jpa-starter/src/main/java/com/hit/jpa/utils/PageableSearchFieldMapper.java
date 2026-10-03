package com.hit.jpa.utils;

import com.hit.common.model.pagination.Order;
import com.hit.common.model.pagination.PageableSearchReqModel;
import com.hit.common.model.query.Filter;
import com.hit.common.model.query.Search;
import com.hit.jpa.exception.QueryException;
import com.querydsl.core.types.Path;

import java.util.List;
import java.util.Map;

/** Maps public field names to JPA entity properties before a pageable search is executed. */
public final class PageableSearchFieldMapper {

    private PageableSearchFieldMapper() {
    }

    public static PageableSearchReqModel toEntityRequest(PageableSearchReqModel request, Map<String, Path<?>> responseFields) {
        PageableSearchReqModel mapped = new PageableSearchReqModel();
        mapped.setPage(request.getPage() + 1);
        mapped.setPageSize(request.getPageSize());
        mapped.setKeyword(request.getKeyword());
        mapped.setSorts(mapOrders(request.getSorts(), responseFields));
        mapped.setFilters(mapFilters(request.getFilters(), responseFields));
        mapped.setOrFilters(mapFilters(request.getOrFilters(), responseFields));
        mapped.setSearches(mapSearches(request.getSearches(), responseFields));
        return mapped;
    }

    private static List<Order> mapOrders(List<Order> orders, Map<String, Path<?>> responseFields) {
        return orders == null || orders.isEmpty() ? null : orders.stream()
                .map(order -> new Order(entityName(order.getName(), responseFields), order.getDirection()))
                .toList();
    }

    private static List<Search> mapSearches(List<Search> searches, Map<String, Path<?>> responseFields) {
        return searches == null || searches.isEmpty() ? null : searches.stream()
                .map(search -> new Search(entityName(search.getName(), responseFields), search.getOption()))
                .toList();
    }

    private static List<Filter> mapFilters(List<Filter> filters, Map<String, Path<?>> responseFields) {
        return filters == null || filters.isEmpty() ? null : filters.stream()
                .map(filter -> new Filter(filter.getOperator(), entityName(filter.getName(), responseFields), filter.getValue()))
                .toList();
    }

    private static String entityName(String responseName, Map<String, Path<?>> responseFields) {
        Path<?> path = responseFields.get(responseName);
        if (path == null) {
            throw new QueryException("Unsupported request field: " + responseName);
        }
        return path.getMetadata().getName();
    }
}

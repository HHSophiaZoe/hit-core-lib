package com.hit.spring.config.bind;

import com.hit.coremodel.pagination.Order;
import com.hit.coremodel.pagination.PageableReqModel;
import com.hit.coremodel.query.Filter;
import com.hit.spring.annotation.PaginationParameter;
import com.hit.spring.core.constant.CommonConstant.CommonSymbol;
import com.hit.spring.core.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
public class PaginationArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterAnnotation(PaginationParameter.class) != null;
    }

    @Override
    public PageableReqModel resolveArgument(MethodParameter methodParameter,
                                            ModelAndViewContainer modelAndViewContainer,
                                            NativeWebRequest nativeWebRequest,
                                            WebDataBinderFactory webDataBinderFactory) {
        HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();
        try {
            Map<String, Object> data = request.getParameterMap().entrySet()
                    .stream()
                    .filter(stringEntry -> stringEntry.getValue().length == 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
            PageableReqModel pageableReqModel = (PageableReqModel) JsonMapper.getObjectMapper().convertValue(data, methodParameter.getParameterType());
            return parser(request.getParameterMap(), pageableReqModel == null ? new PageableReqModel() : pageableReqModel);
        } catch (Exception e) {
            log.error("fail to resolve argument: ", e);
            return parser(request.getParameterMap());
        }
    }

    private static PageableReqModel parser(Map<String, String[]> parameters) {
        return parser(parameters, new PageableReqModel());
    }

    private static PageableReqModel parser(Map<String, String[]> parameters, PageableReqModel request) {
        int page = PageableReqModel.PAGE_NUM_DEFAULT;
        if (parameters.containsKey("page") && isNotEmpty(parameters.get("page")[0])) {
            page = NumberUtils.toInt(parameters.get("page")[0], PageableReqModel.PAGE_NUM_DEFAULT);
        }
        request.setPage(page);

        int pageSize = PageableReqModel.PAGE_SIZE_DEFAULT;
        if (parameters.containsKey("page_size") && isNotEmpty(parameters.get("page_size")[0])) {
            pageSize = NumberUtils.toInt(parameters.get("page_size")[0], PageableReqModel.PAGE_SIZE_DEFAULT);
        } else if (parameters.containsKey("pageSize") && isNotEmpty(parameters.get("pageSize")[0])) {
            pageSize = NumberUtils.toInt(parameters.get("pageSize")[0], PageableReqModel.PAGE_SIZE_DEFAULT);
        }
        pageSize = pageSize < 0 ? PageableReqModel.PAGE_SIZE_DEFAULT : pageSize;
        request.setPageSize(pageSize);

        if (parameters.containsKey("sorts") && isNotEmpty(parameters.get("sorts")[0])) {
            List<Order> orders = getOrders(parameters.get("sorts"));
            if (CollectionUtils.isNotEmpty(orders)) {
                request.setSorts(orders);
            }
        }

        if (parameters.containsKey("filters") && isNotEmpty(parameters.get("filters")[0])) {
            List<Filter> filters = getFilters(parameters.get("filters"));
            if (CollectionUtils.isNotEmpty(filters)) {
                request.setFilters(filters);
            }
        }
        return request;
    }

    private static List<Order> getOrders(String[] orders) {
        return orders == null ? null : Stream.of(orders)
                .filter(Objects::nonNull)
                .map(PaginationArgumentResolver::getOrder)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Order getOrder(String order) {
        String[] arr = order.split(CommonSymbol.COMMA);
        if ( arr.length < 1) {
            return null;
        }
        if (arr.length == 1) {
            return new Order(arr[0], Order.Direction.DESC.name());
        } else {
            return new Order(arr[0], arr[1]);
        }
    }

    private static List<Filter> getFilters(String[] filters) {
        return filters == null ? null : Stream.of(filters)
                .filter(Objects::nonNull)
                .map(PaginationArgumentResolver::getFilter)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Filter getFilter(String filter) {
        String[] arr = filter.split(CommonSymbol.COMMA);
        if (arr.length < 2) {
            return null;
        }

        if (arr.length == 2) {
            return new Filter(arr[0], arr[1]);
        } else {
            return new Filter(arr[0], arr[1], arr[2]);
        }
    }
}

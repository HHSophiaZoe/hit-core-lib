package com.hit.common.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PageUtils {

    public int calTotalPage(Integer totalRecords, Integer pageSize) {
        if (totalRecords == null || totalRecords <= pageSize) {
            return NumberUtils.INTEGER_ONE;
        }
        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    public <T> List<T> paging(List<T> data, Integer page, Integer pageSize) {
        int totalPage = (int) Math.ceil((double) data.size() / pageSize);
        if (totalPage <= page) {
            return new ArrayList<>();
        }
        if ((page + 1) * pageSize >= data.size()) {
            return data.subList(page * pageSize, data.size());
        }
        return data.subList(page * pageSize, (page + 1) * pageSize);
    }

}

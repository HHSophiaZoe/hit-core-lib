package com.hit.spring.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PageUtils {

    public int calTotalPage(Integer totalRecords, Integer pageSize, Integer defaultPage) {
        if (totalRecords == null || totalRecords <= pageSize) {
            return defaultPage;
        }
        return calTotalPage(totalRecords, pageSize);
    }

    public int calTotalPage(Integer totalRecords, Integer pageSize) {
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

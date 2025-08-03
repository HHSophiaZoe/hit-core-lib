package com.hit.common.pagination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> {

    private Long totalElements;

    private Integer totalPages;

    private List<T> contents;

    private boolean hasNext;

    private boolean hasPrevious;

}

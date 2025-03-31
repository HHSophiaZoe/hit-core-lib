package com.hit.coremodel.pagination;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> {

    private Long totalElements;

    private Integer totalPages;

    private List<T> contents;

    private boolean hasNext;

    private boolean hasPrevious;

}

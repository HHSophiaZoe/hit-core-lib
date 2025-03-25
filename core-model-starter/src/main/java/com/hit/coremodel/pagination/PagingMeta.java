package com.hit.coremodel.pagination;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PagingMeta {

    private Long totalElements;

    private Integer totalPages;

    private Integer pageNum;

    private Integer pageSize;

    private Boolean loadMoreAble;

}
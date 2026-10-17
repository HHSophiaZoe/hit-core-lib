package com.hit.jpa.utils;

import com.hit.common.model.pagination.PageResModel;
import com.hit.common.model.pagination.PageableReqModel;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlUtilsTest {

    @Test
    void loadMoreAbleIsTrueOnlyWhenRowsRemainAfterTheCurrentPage() {
        assertThat(meta(1, 10, 25).getLoadMoreAble()).isTrue();   // rows 1-10 of 25
        assertThat(meta(3, 10, 25).getLoadMoreAble()).isFalse();  // rows 21-25 of 25
        assertThat(meta(2, 10, 20).getLoadMoreAble()).isFalse();  // rows 11-20 of 20
        assertThat(meta(1, 10, 11).getLoadMoreAble()).isTrue();
    }

    @Test
    void metaCarriesTotalsAndZeroBasedPage() {
        PageResModel.PagingMeta meta = meta(2, 10, 25);
        assertThat(meta.getTotalElements()).isEqualTo(25);
        assertThat(meta.getTotalPages()).isEqualTo(3);
        assertThat(meta.getPageNum()).isEqualTo(1);
        assertThat(meta.getPageSize()).isEqualTo(10);
    }

    private static PageResModel.PagingMeta meta(int page, int pageSize, long total) {
        PageableReqModel request = new PageableReqModel();
        request.setPage(page);
        request.setPageSize(pageSize);
        PageImpl<String> result = new PageImpl<>(List.of(), PageRequest.of(page - 1, pageSize), total);
        return SqlUtils.buildPagingMeta(request, result);
    }
}

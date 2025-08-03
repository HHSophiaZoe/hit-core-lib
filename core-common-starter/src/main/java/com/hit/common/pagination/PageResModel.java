package com.hit.common.pagination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Data
@NoArgsConstructor
public class PageResModel<T> {

    private PagingMeta meta;

    private List<T> items;

    public PageResModel(PagingMeta meta, List<T> items) {
        this.meta = meta;
        if (CollectionUtils.isEmpty(items)) {
            this.items = Collections.emptyList();
        } else {
            this.items = Collections.unmodifiableList(items);
        }
    }

    public List<T> getItems() {
        return CollectionUtils.isEmpty(items) ? Collections.emptyList() : items;
    }

    public <N> PageResModel<N> map(Function<? super T, ? extends N> mapper) {
        PageResModel<N> newPage = new PageResModel<>();
        newPage.setItems(CollectionUtils.isEmpty(items) ? null : items.stream().<N>map(mapper).toList());
        newPage.setMeta(this.meta);
        return newPage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagingMeta {

        private Long totalElements;

        private Integer totalPages;

        private Integer pageNum;

        private Integer pageSize;

        private Boolean loadMoreAble;

    }
}

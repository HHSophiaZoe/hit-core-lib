package com.hit.coremodel.pagination;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Setter
@Getter
@NoArgsConstructor
public class PaginationResponse<T> {

    private PagingMeta meta;

    private List<T> items;

    public PaginationResponse(PagingMeta meta, List<T> items) {
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

    public <N> PaginationResponse<N> map(Function<? super T, ? extends N> mapper) {
        PaginationResponse<N> newPage = new PaginationResponse<>();
        newPage.setItems(CollectionUtils.isEmpty(items) ? null : items.stream().<N>map(mapper).toList());
        newPage.setMeta(this.meta);
        return newPage;
    }

}

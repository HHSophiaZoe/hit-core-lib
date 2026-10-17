package com.hit.rest.base;

import com.hit.jpa.BaseRepository;
import com.hit.spring.exception.BaseResponseException;
import com.hit.spring.exception.ResponseStatusCodeEnum;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseServiceTest {

    @SuppressWarnings("unchecked")
    private final BaseRepository<String, Long> repository = mock(BaseRepository.class);

    private final BaseService<String, Long, BaseRepository<String, Long>> service = new BaseService<>() {
    };

    {
        service.setRepository(repository);
    }

    @Test
    void deletesWhenEveryIdExists() {
        Set<Long> ids = Set.of(1L, 2L);
        when(repository.getAllId(ids)).thenReturn(List.of(1L, 2L));

        assertThat(service.deleteByIds(ids)).isEqualTo(Boolean.TRUE);
        verify(repository).delete(ids);
    }

    @Test
    void reportsTheIdsThatDoNotExist() {
        Set<Long> ids = new LinkedHashSet<>(List.of(1L, 2L, 3L));
        when(repository.getAllId(ids)).thenReturn(List.of(2L));

        assertThatThrownBy(() -> service.deleteByIds(ids))
                .isInstanceOfSatisfying(BaseResponseException.class, e -> {
                    assertThat(e.getResponseStatusCode()).isEqualTo(ResponseStatusCodeEnum.SHOW_RESOURCES_NOT_FOUND);
                    assertThat(e.getParams()).containsExactly("[1, 3]");
                });
        verify(repository, never()).delete(anyCollection());
    }
}

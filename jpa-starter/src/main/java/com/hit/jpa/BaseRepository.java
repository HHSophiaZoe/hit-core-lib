package com.hit.jpa;

import com.hit.common.pagination.PageableReqModel;
import com.hit.common.pagination.PageResModel;
import com.hit.common.pagination.PageableSearchReqModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BaseRepository<E, ID> {

    PageResModel<E> search(PageableReqModel request);

    PageResModel<E> search(PageableSearchReqModel request);

    List<ID> getAllId();

    List<ID> getAllId(Collection<ID> ids);

    List<E> getAll();

    List<E> getAllByIdIn(Collection<ID> ids);

    Map<ID, E> getMapId(Collection<ID> ids);

    E getOne(ID id);

    boolean exists(ID id);

    E save(E entity);

    void saveAll(Collection<E> entity);

    List<E> saveAllReturning(Collection<E> entity);

    E saveAndFlush(E entity);

    void saveAllAndFlush(Collection<E> entity);

    List<E> saveAllReturningAndFlush(Collection<E> entity);

    E update(E entity);

    void delete(ID id);

    void delete(Collection<ID> ids);

    void deleteBatch(Collection<ID> ids);

}

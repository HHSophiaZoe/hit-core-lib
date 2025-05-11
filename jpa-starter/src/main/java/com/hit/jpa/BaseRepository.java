package com.hit.jpa;

import com.hit.coremodel.pagination.PageableReqModel;
import com.hit.coremodel.pagination.PageResModel;
import com.hit.coremodel.pagination.PageableSearchReqModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BaseRepository<T, ID> {

    PageResModel<T> search(PageableReqModel request);

    PageResModel<T> search(PageableSearchReqModel request);

    List<ID> getAllId();

    List<ID> getAllId(Collection<ID> ids);

    List<T> getAll();

    List<T> getAllByIdIn(Collection<ID> ids);

    Map<ID, T> getMapId(Collection<ID> ids);

    T getOne(ID id);

    boolean exists(ID id);

    T save(T entity);

    List<T> saveAll(Collection<T> entity);

    T saveAndFlush(T entity);

    List<T> saveAllAndFlush(Collection<T> entity);

    T update(T entity);

    void delete(ID id);

    void delete(Collection<ID> ids);

}

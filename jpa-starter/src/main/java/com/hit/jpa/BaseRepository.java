package com.hit.jpa;

import com.hit.coremodel.pagination.PaginationRequest;
import com.hit.coremodel.pagination.PaginationResponse;
import com.hit.coremodel.pagination.PaginationSearchRequest;
import com.querydsl.core.types.Predicate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BaseRepository<T, ID> {

    PaginationResponse<T> search(PaginationRequest request);

    PaginationResponse<T> search(PaginationRequest request, Predicate condition);

    PaginationResponse<T> search(PaginationSearchRequest request);

    PaginationResponse<T> search(PaginationSearchRequest request, Predicate condition);

    List<ID> getAllId();

    List<ID> getAllId(Collection<ID> ids);

    List<ID> getAllId(Predicate condition);

    List<T> getAll();

    List<T> getAllByIdIn(Collection<ID> ids);

    Map<ID, T> getMapId(Collection<ID> ids);

    T getOne(ID id);

    T getOne(Predicate condition);

    boolean exists(ID id);

    T save(T entity);

    List<T> saveAll(Collection<T> entity);

    T saveAndFlush(T entity);

    List<T> saveAllAndFlush(Collection<T> entity);

    T update(T entity);

    void delete(ID id);

    void delete(Collection<ID> ids);

}

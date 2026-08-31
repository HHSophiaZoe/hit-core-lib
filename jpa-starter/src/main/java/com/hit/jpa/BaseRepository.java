package com.hit.jpa;

import com.hit.common.model.pagination.PageableReqModel;
import com.hit.common.model.pagination.PageResModel;
import com.hit.common.model.pagination.PageableSearchReqModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BaseRepository<M, ID> {

    PageResModel<M> search(PageableReqModel request);

    PageResModel<M> search(PageableSearchReqModel request);

    List<ID> getAllId();

    List<ID> getAllId(Collection<ID> ids);

    List<M> getAll();

    long count();

    List<M> getAllByIdIn(Collection<ID> ids);

    Map<ID, M> getMapId(Collection<ID> ids);

    M getOne(ID id);

    boolean exists(ID id);

    M save(M model);

    void saveAll(Collection<M> models);

    List<M> saveAllReturning(Collection<M> models);

    M saveAndFlush(M model);

    void saveAllAndFlush(Collection<M> models);

    List<M> saveAllReturningAndFlush(Collection<M> models);

    M update(M model);

    void delete(ID id);

    void delete(Collection<ID> ids);

    void deleteBatch(Collection<ID> ids);

    void deleteAllInBatch();

    void flush();

}

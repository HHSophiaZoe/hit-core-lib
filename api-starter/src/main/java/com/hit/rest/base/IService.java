package com.hit.rest.base;

import com.hit.common.pagination.PageResModel;
import com.hit.common.pagination.PageableReqModel;
import com.hit.common.pagination.PageableSearchReqModel;

import java.util.List;
import java.util.Set;

public interface IService<M, ID> {

    M getById(ID id);

    M getBasicById(ID id);

    List<M> getByIds(List<ID> ids);

    PageResModel<M> select(PageableReqModel request);

    PageResModel<M> search(PageableSearchReqModel request);

    Object deleteById(ID id);

    Object deleteByIds(Set<ID> ids);

}
package com.hit.rest.base;


import com.hit.coremodel.pagination.PaginationRequest;
import com.hit.coremodel.pagination.PaginationResponse;
import com.hit.coremodel.pagination.PaginationSearchRequest;
import com.hit.spring.core.data.response.CommonResponse;

import java.util.List;
import java.util.Set;

public interface IService<RS, ID> {

    RS getById(ID id);

    List<RS> getByIds(List<ID> ids);

    PaginationResponse<RS> select(PaginationRequest request);

    PaginationResponse<RS> search(PaginationSearchRequest request);

    CommonResponse deleteById(ID id);

    CommonResponse deleteByIds(Set<ID> ids);

}
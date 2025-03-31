package com.hit.rest.base;

import com.hit.coremodel.pagination.PaginationRequest;
import com.hit.coremodel.pagination.PaginationResponse;
import com.hit.coremodel.pagination.PaginationSearchRequest;
import com.hit.jpa.BaseRepository;
import com.hit.spring.core.constants.MessageResponse;
import com.hit.spring.core.data.mapper.ResponseMapper;
import com.hit.spring.core.data.response.CommonResponse;
import com.hit.spring.core.exception.BaseResponseException;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class BaseService<RS, E, ID, Repo extends BaseRepository<E, ID>, Map extends ResponseMapper<RS, E>> implements IService<RS, ID> {

    @Setter(onMethod_ = {@Autowired})
    protected Map mapper;

    @Setter(onMethod_ = {@Autowired})
    protected Repo repository;

    public RS getById(ID id) {
        E e = repository.getOne(id);
        if (e == null) throw new BaseResponseException(ResponseStatusCodeEnum.RESOURCE_NOT_FOUND);
        return mapper.toResponse(e);
    }

    public List<RS> getByIds(List<ID> ids) {
        List<E> list = repository.getAllByIdIn(ids);
        return mapper.toResponses(list);
    }

    public PaginationResponse<RS> select(PaginationRequest request) {
        PaginationResponse<E> poPaginationResponse = repository.search(request);
        return poPaginationResponse.map(mapper::toResponse);
    }

    public PaginationResponse<RS> search(PaginationSearchRequest request) {
        PaginationResponse<E> poPaginationResponse = repository.search(request);
        return poPaginationResponse.map(mapper::toResponse);
    }

    @Override
    public CommonResponse deleteById(ID id) {
        repository.delete(id);
        return new CommonResponse(Boolean.TRUE, MessageResponse.DELETE_SUCCESS);
    }

    @Override
    public CommonResponse deleteByIds(Set<ID> ids) {
        List<ID> allId = repository.getAllId(ids);
        if (allId.size() == ids.size()) {
            repository.delete(ids);
            return new CommonResponse(Boolean.TRUE, MessageResponse.DELETE_SUCCESS);
        }

        List<ID> idNotExists = new ArrayList<>();
        for (ID id : allId) {
            if (!ids.contains(id)) {
                idNotExists.add(id);
            }
        }
        throw new BaseResponseException(ResponseStatusCodeEnum.DELETE_RESOURCES_NOT_FOUND, new String[]{idNotExists.toString()});
    }

}

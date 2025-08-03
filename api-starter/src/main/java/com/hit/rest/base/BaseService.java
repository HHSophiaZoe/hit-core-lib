package com.hit.rest.base;

import com.hit.common.pagination.PageResModel;
import com.hit.common.pagination.PageableReqModel;
import com.hit.common.pagination.PageableSearchReqModel;
import com.hit.jpa.BaseRepository;
import com.hit.spring.core.data.mapper.DomainMapper;
import com.hit.spring.core.exception.BaseResponseException;
import com.hit.spring.core.exception.ResponseStatusCodeEnum;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class BaseService<M, E, ID, Repo extends BaseRepository<E, ID>, Map extends DomainMapper<E, M>> implements IService<M, ID> {

    @Setter(onMethod_ = {@Autowired})
    protected Map mapper;

    @Setter(onMethod_ = {@Autowired})
    protected Repo repository;

    @Override
    @Transactional
    public M getById(ID id) {
        E e = repository.getOne(id);
        if (e == null) throw new BaseResponseException(ResponseStatusCodeEnum.RESOURCE_NOT_FOUND);
        return mapper.toModel(e);
    }

    @Override
    @Transactional
    public M getBasicById(ID id) {
        E e = repository.getOne(id);
        if (e == null) throw new BaseResponseException(ResponseStatusCodeEnum.RESOURCE_NOT_FOUND);
        return mapper.toBasicModel(e);
    }

    @Override
    @Transactional
    public List<M> getByIds(List<ID> ids) {
        List<E> list = repository.getAllByIdIn(ids);
        return mapper.toModels(list);
    }

    @Override
    @Transactional
    public PageResModel<M> select(PageableReqModel request) {
        PageResModel<E> poPageResModel = repository.search(request);
        return poPageResModel.map(mapper::toModel);
    }

    @Override
    @Transactional
    public PageResModel<M> search(PageableSearchReqModel request) {
        PageResModel<E> poPageResModel = repository.search(request);
        return poPageResModel.map(mapper::toModel);
    }

    @Override
    @Transactional
    public Object deleteById(ID id) {
        repository.delete(id);
        return Boolean.TRUE;
    }

    @Override
    @Transactional
    public Object deleteByIds(Set<ID> ids) {
        List<ID> allId = repository.getAllId(ids);
        if (allId.size() == ids.size()) {
            repository.delete(ids);
            return Boolean.TRUE;
        }

        List<ID> idNotExists = new ArrayList<>();
        for (ID id : allId) {
            if (!ids.contains(id)) {
                idNotExists.add(id);
            }
        }
        throw new BaseResponseException(ResponseStatusCodeEnum.SHOW_RESOURCES_NOT_FOUND, new String[]{idNotExists.toString()});
    }

}

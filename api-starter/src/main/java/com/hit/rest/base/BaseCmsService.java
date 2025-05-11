package com.hit.rest.base;

import com.hit.jpa.BaseRepository;
import com.hit.spring.core.data.mapper.DomainMapper;
import com.hit.rest.model.ItemPermission;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCmsService<M extends ItemPermission, E, ID, Repo extends BaseRepository<E, ID>, Map extends DomainMapper<E, M>>
        extends BaseService<M, E, ID, Repo, Map> implements IService<M, ID> {

    // Processing

}

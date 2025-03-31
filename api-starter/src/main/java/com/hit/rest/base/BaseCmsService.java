package com.hit.rest.base;

import com.hit.jpa.BaseRepository;
import com.hit.spring.core.data.mapper.ResponseMapper;
import com.hit.spring.core.data.response.ItemPermissionResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCmsService<RS extends ItemPermissionResponse, E, ID, Repo extends BaseRepository<E, ID>, Map extends ResponseMapper<RS, E>>
        extends BaseService<RS, E, ID, Repo, Map> implements IService<RS, ID> {

    // Processing

}

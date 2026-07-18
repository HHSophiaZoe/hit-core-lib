package com.hit.rest.base;

import com.hit.jpa.BaseRepository;
import com.hit.rest.model.ItemPermission;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseCmsService<M extends ItemPermission, ID, Repo extends BaseRepository<M, ID>> extends BaseService<M, ID, Repo> implements IService<M, ID> {

    // Processing

}

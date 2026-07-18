package com.hit.common.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;

public interface DomainMapper<E, M> {

    @Named("toEntity")
    E toEntity(M model);

    @IterableMapping(qualifiedByName = "toEntity")
    List<E> toEntities(Collection<M> models);

    @Named("toModel")
    M toModel(E entity);

    @IterableMapping(qualifiedByName = "toModel")
    List<M> toModels(Collection<E> entities);

}

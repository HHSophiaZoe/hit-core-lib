package com.hit.common.mapper;

import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.List;

public interface DomainMapper<E, M> {

    E toEntity(M model);

    void updateEntity(M model, @MappingTarget E entity);

    List<E> toEntities(Collection<M> models);

    M toModel(E entity);

    List<M> toModels(Collection<E> entities);

}

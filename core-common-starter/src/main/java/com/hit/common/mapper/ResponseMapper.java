package com.hit.common.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

public interface ResponseMapper<M, R> {

    @Named("toResponse")
    R toResponse(M model);

    @IterableMapping(qualifiedByName = "toResponse")
    List<R> toResponses(List<M> models);

}

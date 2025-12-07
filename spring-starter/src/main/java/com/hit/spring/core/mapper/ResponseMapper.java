package com.hit.spring.core.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

public interface ResponseMapper<M, Rs> {

    @Named("toResponse")
    Rs toResponse(M e);

    @IterableMapping(qualifiedByName = "toResponse")
    List<Rs> toResponses(List<M> list);

}

package com.hit.spring.core.data.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.util.List;

public interface ResponseMapper<Rs, E> {

    @Named("toResponse")
    Rs toResponse(E e);

    @IterableMapping(qualifiedByName = "toResponse")
    List<Rs> toResponses(List<E> list);

}

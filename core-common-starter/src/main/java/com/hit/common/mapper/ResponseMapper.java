package com.hit.common.mapper;

import java.util.List;

public interface ResponseMapper<M, R> {

    R toResponse(M model);

    List<R> toResponses(List<M> models);

}

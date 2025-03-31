package com.hit.jpa.querydsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface QueryDSLRepository<T, ID> {

    <X> List<X> executeQuery(Function<JPAQueryFactory, JPAQuery<X>> queryFunction);

    List<ID> getAllId();

    List<ID> getAllId(Collection<ID> ids);

    List<ID> getAllId(Predicate condition);

    List<T> getAll();

    List<T> getAllByIdIn(Collection<ID> ids);

    List<T> getAll(Predicate condition);

    List<T> getAllOrders(Predicate condition, OrderSpecifier<?>... orders);

    Map<ID, T> getMap();

    Map<ID, T> getMapByIdIn(Collection<ID> ids);

    Map<ID, T> getMapByCondition(Predicate condition);

    Optional<T> getOne(Predicate condition);

    Long count(Predicate condition);

    boolean exists(Predicate condition);

}

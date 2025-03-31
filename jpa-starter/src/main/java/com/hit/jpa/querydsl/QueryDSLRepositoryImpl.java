package com.hit.jpa.querydsl;

import com.hit.jpa.exception.DBException;
import com.hit.jpa.utils.SqlUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class QueryDSLRepositoryImpl<T, ID> implements QueryDSLRepository<T, ID> {

    private final EntityManager entityManager;

    private final EntityPathBase<T> entityPath;

    private final Field fieldID;

    private final JPAQueryFactory queryFactory;

    public QueryDSLRepositoryImpl(EntityManager entityManager, EntityPathBase<T> entityPath, Field fieldID) {
        this.entityManager = entityManager;
        this.entityPath = entityPath;
        this.fieldID = fieldID;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public <X> List<X> executeQuery(Function<JPAQueryFactory, JPAQuery<X>> queryFunction) {
        return queryFunction.apply(queryFactory).fetch();
    }

    @Override
    public List<ID> getAllId() {
        return this.getAllId(new BooleanBuilder());
    }

    @Override
    public List<ID> getAllId(Collection<ID> ids) {
        PathBuilder<T> pathBuilder = new PathBuilder<>(this.getEntityPath().getType(), this.getEntityPath().getMetadata().getName());
        SimplePath<ID> idPath = Expressions.path((Class<ID>) fieldID.getType(), pathBuilder, fieldID.getName());
        return queryFactory
                .select(idPath)
                .from(this.getEntityPath())
                .where(idPath.in(ids))
                .fetch();
    }

    @Override
    public List<ID> getAllId(Predicate condition) {
        PathBuilder<T> pathBuilder = new PathBuilder<>(this.getEntityPath().getType(), this.getEntityPath().getMetadata().getName());
        return queryFactory
                .select(Expressions.path((Class<ID>) fieldID.getType(), pathBuilder, fieldID.getName()))
                .from(this.getEntityPath())
                .where(condition)
                .fetch();
    }

    @Override
    public List<T> getAll() {
        return queryFactory.selectFrom(getEntityPath()).fetch();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllByIdIn(Collection<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        PathBuilder<T> pathBuilder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata().getName());
        if (Comparable.class.isAssignableFrom(fieldID.getType())) {
            ComparablePath<Comparable<?>> idPath = Expressions.comparablePath((Class<Comparable<?>>) fieldID.getType(), pathBuilder, fieldID.getName());
            return queryFactory
                    .selectFrom(entityPath)
                    .where(idPath.in((Collection<? extends Comparable<?>>) ids))
                    .fetch();
        } else {
            SimplePath<ID> idPath = Expressions.path((Class<ID>) fieldID.getType(), pathBuilder, fieldID.getName());
            return queryFactory
                    .selectFrom(entityPath)
                    .where(idPath.in(ids))
                    .fetch();
        }
    }

    @Override
    public List<T> getAll(Predicate condition) {
        return queryFactory
                .selectFrom(getEntityPath())
                .where(condition)
                .fetch();
    }

    @Override
    public List<T> getAllOrders(Predicate condition, OrderSpecifier<?>... orders) {
        JPAQuery<T> query = queryFactory
                .selectFrom(getEntityPath())
                .where(condition);

        if (orders != null && orders.length > 0) {
            query.orderBy(orders);
        }

        return query.fetch();
    }

    @Override
    public Map<ID, T> getMap() {
        List<T> entities = getAll();
        return entitiesToMap(entities);
    }

    @Override
    public Map<ID, T> getMapByIdIn(Collection<ID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<T> entities = getAllByIdIn(ids);
        return entitiesToMap(entities);
    }

    @Override
    public Map<ID, T> getMapByCondition(Predicate condition) {
        List<T> entities = getAll(condition);
        return entitiesToMap(entities);
    }

    @Override
    public Optional<T> getOne(Predicate condition) {
        return Optional.ofNullable(queryFactory.selectFrom(getEntityPath())
                .where(condition)
                .fetchOne());
    }

    @Override
    public Long count(Predicate condition) {
        return queryFactory
                .selectFrom(getEntityPath())
                .where(condition)
                .fetchCount();
    }

    @Override
    public boolean exists(Predicate condition) {
        return queryFactory
                .selectOne()
                .from(entityPath)
                .where(condition)
                .fetchCount() > 0;
    }

    private Map<ID, T> entitiesToMap(List<T> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyMap();
        }
        return entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        entity -> {
                            try {
                                return SqlUtils.getEntityId(entityManager, entity);
                            } catch (Exception e) {
                                throw new DBException("Cannot access ID", e);
                            }
                        },
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

}
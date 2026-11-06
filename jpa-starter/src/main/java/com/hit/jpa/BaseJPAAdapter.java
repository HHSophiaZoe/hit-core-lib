package com.hit.jpa;

import com.hit.common.mapper.DomainMapper;

/**
 * Adapter JPA khi khóa của model chính là ID của entity (ID đơn giản như {@code Long}, {@code UUID}, {@code String}). ID ghép
 * dùng {@link AbstractJPAAdapter} để model giữ khóa riêng, không phụ thuộc class {@code @Embeddable} của JPA.
 */
public abstract class BaseJPAAdapter<M, E, ID, R extends BaseJPARepository<E, ID>> extends AbstractJPAAdapter<M, E, ID, ID, R> {

    protected BaseJPAAdapter(Class<E> entityClass, R jpaRepository, DomainMapper<E, M> mapper) {
        super(entityClass, jpaRepository, mapper);
    }

    @Override
    protected final ID toEntityId(ID key) {
        return key;
    }

    @Override
    protected final ID toModelKey(ID id) {
        return id;
    }
}

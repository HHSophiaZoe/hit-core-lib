package com.hit.jpa;

import com.hit.coremodel.pagination.PaginationRequest;
import com.hit.coremodel.pagination.PaginationResponse;
import com.hit.coremodel.pagination.PaginationSearchRequest;
import com.hit.jpa.querydsl.QueryDSLRepository;
import com.hit.jpa.querydsl.QueryDSLRepositoryImpl;
import com.hit.jpa.utils.SqlUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public abstract class BaseJPAAdapter<T, ID, R extends BaseJPARepository<T, ID>> implements BaseRepository<T, ID> {

    @PersistenceContext(unitName = "defaultEntityManager")
    private EntityManager entityManager;

    @Setter(onMethod_ = {@Autowired})
    protected R jpaRepository;

    protected QueryDSLRepository<T, ID> dslRepository;

    protected Field fieldID;

    @PostConstruct
    private void init() {
        Metamodel metamodel = entityManager.getMetamodel();
        EntityType<T> entityType = metamodel.entity(this.getEntityClass());
        SingularAttribute<? super T, ?> idAttribute = entityType.getId(Object.class);
        if (idAttribute.getJavaMember() instanceof Field idField) {
            this.fieldID = idField;
        } else {
            this.fieldID = (Field) idAttribute.getJavaMember();
        }
        this.dslRepository = new QueryDSLRepositoryImpl<>(getEntityManager(), getEntityPath(), fieldID);
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected abstract EntityPathBase<T> getEntityPath();

    protected abstract Class<T> getEntityClass();

    @Override
    public PaginationResponse<T> search(PaginationRequest request) {
        Pageable pageable = SqlUtils.createPageable(request);
        Specification<T> specification = SqlUtils.createSpecificationPagination(request, this.getEntityClass());
        Page<T> page = this.jpaRepository.findAll(specification, pageable);
        return new PaginationResponse<>(SqlUtils.buildPagingMeta(request, page), page.getContent());
    }

    @Override
    public PaginationResponse<T> search(PaginationRequest request, Predicate condition) {
        return null;
    }

    public PaginationResponse<T> search(PaginationSearchRequest request) {
        Pageable pageable = SqlUtils.createPageable(request);
        Specification<T> specification = SqlUtils.createSpecificationPaginationSearch(request, this.getEntityClass());
        Page<T> page = this.jpaRepository.findAll(specification, pageable);
        return new PaginationResponse<>(SqlUtils.buildPagingMeta(request, page), page.getContent());
    }

    @Override
    public PaginationResponse<T> search(PaginationSearchRequest request, Predicate condition) {
        return null;
    }

    @Override
    public List<ID> getAllId() {
        return this.dslRepository.getAllId();
    }

    @Override
    public List<ID> getAllId(Collection<ID> ids) {
        return this.dslRepository.getAllId(ids);
    }

    @Override
    public List<ID> getAllId(Predicate condition) {
        return this.dslRepository.getAllId(condition);
    }

    @Override
    public List<T> getAll() {
        return this.jpaRepository.findAll();
    }

    @Override
    public List<T> getAllByIdIn(Collection<ID> ids) {
        return this.jpaRepository.findAllById(ids);
    }

    @Override
    public Map<ID, T> getMapId(Collection<ID> ids) {
        List<T> entities = this.jpaRepository.findAllById(ids);
        return entities.stream().collect(Collectors.toMap(
                item -> SqlUtils.getEntityId(this.getEntityManager(), item),
                Function.identity()
        ));
    }

    @Override
    public T getOne(ID id) {
        return this.jpaRepository.findById(id).orElse(null);
    }

    @Override
    public T getOne(Predicate condition) {
        return this.dslRepository.getOne(condition).orElse(null);
    }

    @Override
    public boolean exists(ID id) {
        return this.jpaRepository.existsById(id);
    }

    @Override
    public T save(T entity) {
        return this.jpaRepository.save(entity);
    }

    @Override
    public List<T> saveAll(Collection<T> entity) {
        return this.jpaRepository.saveAll(entity);
    }

    @Override
    public T saveAndFlush(T entity) {
        return this.jpaRepository.saveAndFlush(entity);
    }

    @Override
    public List<T> saveAllAndFlush(Collection<T> entity) {
        List<T> temp = this.jpaRepository.saveAll(entity);
        this.jpaRepository.flush();
        return temp;
    }

    @Override
    public T update(T entity) {
        return this.jpaRepository.save(entity);
    }

    @Override
    public void delete(ID id) {
        this.jpaRepository.deleteById(id);
    }

    @Override
    public void delete(Collection<ID> ids) {
        this.jpaRepository.deleteAllById(ids);
    }
}

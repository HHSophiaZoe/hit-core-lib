package com.hit.jpa;

import com.hit.common.pagination.PageResModel;
import com.hit.common.pagination.PageableReqModel;
import com.hit.common.pagination.PageableSearchReqModel;
import com.hit.jpa.utils.ChunkUtils;
import com.hit.jpa.utils.SqlUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public abstract class BaseJPAAdapter<E, ID, R extends BaseJPARepository<E, ID>> implements BaseRepository<E, ID> {

    @PersistenceContext(unitName = "defaultEntityManager")
    private EntityManager entityManager;

    @Setter(onMethod_ = {@Autowired})
    protected R jpaRepository;

    protected Field columnID;

    protected Set<String> allColumnEntity;

    protected EntityPath<E> entityPath;

    protected JPAQueryFactory queryFactory;

    protected static final Integer DEFAULT_BATCH_DELETE = 500;

    @SneakyThrows
    @PostConstruct
    private void init() {
        Metamodel metamodel = getEntityManager().getMetamodel();
        EntityType<E> entityType = metamodel.entity(this.getEntityClass());
        SingularAttribute<? super E, ?> idAttribute = entityType.getId(Object.class);
        if (idAttribute.getJavaMember() instanceof Field idField) {
            this.columnID = idField;
        } else {
            this.columnID = (Field) idAttribute.getJavaMember();
        }

        EntityPathResolver entityPathResolver = new SimpleEntityPathResolver(StringUtils.EMPTY);
        this.entityPath = entityPathResolver.createPath(this.getEntityClass());
        this.queryFactory = new JPAQueryFactory(this.getEntityManager());

        this.allColumnEntity = new HashSet<>();
        // Get all fields from the QueryDSL entity path class
        Field[] fields = this.entityPath.getClass().getFields();
        for (Field field : fields) {
            // Skip static fields and non-Path fields
            if (Modifier.isStatic(field.getModifiers()) || !Path.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Path<?> path = (Path<?>) field.get(this.entityPath);
            if (path != null) {
                this.allColumnEntity.add(path.getMetadata().getName());
            }
        }
    }

    protected EntityManager getEntityManager() {
        return this.entityManager;
    }

    protected abstract Class<E> getEntityClass();

    @SneakyThrows
    protected Set<String> getPageableColumnAccess() {
        return this.allColumnEntity;
    }

    @Override
    public PageResModel<E> search(PageableReqModel request) {
        Pageable pageable = SqlUtils.createPageable(request);
        Specification<E> specification = SqlUtils.createSpecificationPagination(request, this.getEntityClass(), this.getPageableColumnAccess());
        Page<E> page = this.jpaRepository.findAll(specification, pageable);
        return new PageResModel<>(SqlUtils.buildPagingMeta(request, page), page.getContent());
    }

    @Override
    public PageResModel<E> search(PageableSearchReqModel request) {
        Pageable pageable = SqlUtils.createPageable(request);
        Specification<E> specification = SqlUtils.createSpecificationPaginationSearch(request, this.getEntityClass(), this.getPageableColumnAccess());
        Page<E> page = this.jpaRepository.findAll(specification, pageable);
        return new PageResModel<>(SqlUtils.buildPagingMeta(request, page), page.getContent());
    }

    @Override
    public List<ID> getAllId() {
        return this.getAllId(new BooleanBuilder());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ID> getAllId(Collection<ID> ids) {
        PathBuilder<E> pathBuilder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata().getName());
        SimplePath<ID> idPath = Expressions.path((Class<ID>) columnID.getType(), pathBuilder, columnID.getName());
        return this.queryFactory.query()
                .select(idPath)
                .from(entityPath)
                .where(idPath.in(ids))
                .fetch();
    }

    @SuppressWarnings("unchecked")
    protected List<ID> getAllId(Predicate condition) {
        PathBuilder<E> pathBuilder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata().getName());
        return this.queryFactory.query()
                .select(Expressions.path((Class<ID>) columnID.getType(), pathBuilder, columnID.getName()))
                .from(entityPath)
                .where(condition)
                .fetch();
    }

    @Override
    public List<E> getAll() {
        return this.jpaRepository.findAll();
    }

    protected List<E> getAll(Predicate condition) {
        return queryFactory.selectFrom(entityPath)
                .where(condition)
                .fetch();
    }

    @Override
    public List<E> getAllByIdIn(Collection<ID> ids) {
        return this.jpaRepository.findAllById(ids);
    }

    @Override
    public Map<ID, E> getMapId(Collection<ID> ids) {
        List<E> entities = this.jpaRepository.findAllById(ids);
        return entities.stream().collect(Collectors.toMap(
                item -> SqlUtils.getEntityId(this.getEntityManager(), item),
                Function.identity()
        ));
    }

    @Override
    public E getOne(ID id) {
        return this.jpaRepository.findById(id).orElse(null);
    }

    protected E getOne(Predicate condition) {
        return this.jpaRepository.findOne(condition).orElse(null);
    }

    @Override
    public boolean exists(ID id) {
        return this.jpaRepository.existsById(id);
    }

    @Override
    public E save(E entity) {
        return this.jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void saveAll(Collection<E> entities) {
        for (E entity : entities) {
            this.save(entity);
        }
    }

    @Override
    public List<E> saveAllReturning(Collection<E> entities) {
        return this.jpaRepository.saveAll(entities);
    }

    @Override
    public E saveAndFlush(E entity) {
        return this.jpaRepository.saveAndFlush(entity);
    }

    @Override
    @Transactional
    public void saveAllAndFlush(Collection<E> entities) {
        for (E entity : entities) {
            this.save(entity);
        }
        this.jpaRepository.flush();
    }

    @Override
    @Transactional
    public List<E> saveAllReturningAndFlush(Collection<E> entities) {
        List<E> temp = this.jpaRepository.saveAll(entities);
        this.jpaRepository.flush();
        return temp;
    }

    @Override
    public E update(E entity) {
        return this.jpaRepository.save(entity);
    }

    @Override
    public void delete(ID id) {
        this.jpaRepository.deleteById(id);
    }

    @Override
    public void delete(Collection<ID> ids) {
        ChunkUtils.toChunks(ids, DEFAULT_BATCH_DELETE).forEach(chunk -> {
            this.jpaRepository.deleteAllById(chunk.getItems());
            log.debug("Delete {} entities size:{}, from:{}, to:{}", chunk.getItems().size(), chunk.getSize(), chunk.getFrom(), chunk.getTo());
        });
    }

    @Override
    public void deleteBatch(Collection<ID> ids) {
        ChunkUtils.toChunks(ids, DEFAULT_BATCH_DELETE).forEach(chunk -> {
            this.jpaRepository.deleteAllByIdInBatch(chunk.getItems());
            log.debug("Delete batch {} entities size:{}, from:{}, to:{}", chunk.getItems().size(), chunk.getSize(), chunk.getFrom(), chunk.getTo());
        });
    }
}

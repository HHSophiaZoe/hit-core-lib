package com.hit.jpa;

import com.hit.common.model.pagination.PageResModel;
import com.hit.common.model.pagination.PageableReqModel;
import com.hit.common.model.pagination.PageableSearchReqModel;
import com.hit.jpa.utils.ChunkUtils;
import com.hit.jpa.utils.SqlUtils;
import com.hit.common.mapper.DomainMapper;
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
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter JPA dùng chung, cho phép khóa của model ({@code K}) khác ID của entity ({@code ID}): model không cần biết class ID
 * {@code @Embeddable} của JPA. Lớp con đổi qua lại bằng {@link #toEntityId} và {@link #toModelKey}. Khóa trùng ID thì dùng
 * {@link BaseJPAAdapter}.
 */
@Slf4j
public abstract class AbstractJPAAdapter<M, E, K, ID, R extends BaseJPARepository<E, ID>> implements BaseRepository<M, K> {

    @PersistenceContext(unitName = "defaultEntityManager")
    private EntityManager entityManager;

    protected final R jpaRepository;

    protected final DomainMapper<E, M> mapper;

    protected final Class<E> entityClass;

    protected Field columnID;

    protected Set<String> allColumnEntity;

    protected EntityPath<E> entityPath;

    protected JPAQueryFactory queryFactory;

    protected static final Integer DEFAULT_BATCH_DELETE = 500;

    protected AbstractJPAAdapter(Class<E> entityClass, R jpaRepository, DomainMapper<E, M> mapper) {
        this.entityClass = entityClass;
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @SneakyThrows
    @PostConstruct
    private void init() {
        Metamodel metamodel = getEntityManager().getMetamodel();
        EntityType<E> entityType = metamodel.entity(this.entityClass);
        SingularAttribute<? super E, ?> idAttribute = entityType.getId(Object.class);
        if (idAttribute.getJavaMember() instanceof Field idField) {
            this.columnID = idField;
        } else {
            this.columnID = (Field) idAttribute.getJavaMember();
        }

        EntityPathResolver entityPathResolver = new SimpleEntityPathResolver(StringUtils.EMPTY);
        this.entityPath = entityPathResolver.createPath(this.entityClass);
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

    /** Khóa của model → ID của entity. */
    protected abstract ID toEntityId(K key);

    /** ID của entity → khóa của model. */
    protected abstract K toModelKey(ID id);

    private List<ID> toEntityIds(Collection<K> keys) {
        return keys.stream().map(this::toEntityId).toList();
    }

    protected EntityManager getEntityManager() {
        return this.entityManager;
    }

    @SneakyThrows
    protected Set<String> getPageableColumnAccess() {
        return this.allColumnEntity;
    }

    @Override
    public PageResModel<M> search(PageableReqModel request) {
        Pageable pageable = SqlUtils.createPageable(request);
        Specification<E> specification = SqlUtils.createSpecificationPagination(request, this.entityClass, this.getPageableColumnAccess());
        Page<E> page = this.jpaRepository.findAll(specification, pageable);
        return new PageResModel<>(SqlUtils.buildPagingMeta(request, page), this.mapper.toModels(page.getContent()));
    }

    @Override
    public PageResModel<M> search(PageableSearchReqModel request) {
        Pageable pageable = SqlUtils.createPageable(request);
        Specification<E> specification = SqlUtils.createSpecificationPaginationSearch(request, this.entityClass, this.getPageableColumnAccess());
        Page<E> page = this.jpaRepository.findAll(specification, pageable);
        return new PageResModel<>(SqlUtils.buildPagingMeta(request, page), this.mapper.toModels(page.getContent()));
    }

    @Override
    public List<K> getAllId() {
        return this.getAllId(new BooleanBuilder());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<K> getAllId(Collection<K> keys) {
        PathBuilder<E> pathBuilder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata().getName());
        SimplePath<ID> idPath = Expressions.path((Class<ID>) columnID.getType(), pathBuilder, columnID.getName());
        return this.queryFactory.query()
                .select(idPath)
                .from(entityPath)
                .where(idPath.in(this.toEntityIds(keys)))
                .fetch()
                .stream()
                .map(this::toModelKey)
                .toList();
    }

    @SuppressWarnings("unchecked")
    protected List<K> getAllId(Predicate condition) {
        PathBuilder<E> pathBuilder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata().getName());
        return this.queryFactory.query()
                .select(Expressions.path((Class<ID>) columnID.getType(), pathBuilder, columnID.getName()))
                .from(entityPath)
                .where(condition)
                .fetch()
                .stream()
                .map(this::toModelKey)
                .toList();
    }

    @Override
    public List<M> getAll() {
        return this.mapper.toModels(this.jpaRepository.findAll());
    }

    @Override
    public long count() {
        return this.jpaRepository.count();
    }

    protected List<E> getAll(Predicate condition) {
        return queryFactory.selectFrom(entityPath)
                .where(condition)
                .fetch();
    }

    @Override
    public List<M> getAllByIdIn(Collection<K> keys) {
        return this.mapper.toModels(this.jpaRepository.findAllById(this.toEntityIds(keys)));
    }

    @Override
    public Map<K, M> getMapId(Collection<K> keys) {
        List<E> entities = this.jpaRepository.findAllById(this.toEntityIds(keys));
        return entities.stream().collect(Collectors.toMap(
                item -> this.toModelKey(SqlUtils.getEntityId(this.getEntityManager(), item)),
                this.mapper::toModel
        ));
    }

    @Override
    public M getOne(K key) {
        return this.jpaRepository.findById(this.toEntityId(key)).map(this.mapper::toModel).orElse(null);
    }

    protected E getOne(Predicate condition) {
        return this.jpaRepository.findOne(condition).orElse(null);
    }

    @Override
    public boolean exists(K key) {
        return this.jpaRepository.existsById(this.toEntityId(key));
    }

    @Override
    public M save(M model) {
        return this.mapper.toModel(this.jpaRepository.save(this.mapper.toEntity(model)));
    }

    @Override
    @Transactional
    public void saveAll(Collection<M> models) {
        this.jpaRepository.saveAll(this.mapper.toEntities(models));
    }

    @Override
    public List<M> saveAllReturning(Collection<M> models) {
        return this.mapper.toModels(this.jpaRepository.saveAll(this.mapper.toEntities(models)));
    }

    @Override
    public M saveAndFlush(M model) {
        return this.mapper.toModel(this.jpaRepository.saveAndFlush(this.mapper.toEntity(model)));
    }

    @Override
    @Transactional
    public void saveAllAndFlush(Collection<M> models) {
        this.jpaRepository.saveAllAndFlush(this.mapper.toEntities(models));
    }

    @Override
    @Transactional
    public List<M> saveAllReturningAndFlush(Collection<M> models) {
        List<E> temp = this.jpaRepository.saveAll(this.mapper.toEntities(models));
        this.jpaRepository.flush();
        return this.mapper.toModels(temp);
    }

    @Override
    @Transactional
    public M update(M model) {
        E source = this.mapper.toEntity(model);
        ID id = SqlUtils.getEntityId(this.getEntityManager(), source);
        if (id == null) {
            throw new IllegalArgumentException("Cannot update entity without an identifier");
        }

        E entity = this.jpaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("%s with id %s not found".formatted(this.entityClass.getSimpleName(), id)));
        this.mapper.updateEntity(model, entity);
        return this.mapper.toModel(entity);
    }

    @Override
    public void delete(K key) {
        this.jpaRepository.deleteById(this.toEntityId(key));
    }

    @Override
    public void delete(Collection<K> keys) {
        ChunkUtils.toChunks(this.toEntityIds(keys), DEFAULT_BATCH_DELETE).forEach(chunk -> {
            this.jpaRepository.deleteAllById(chunk.getItems());
            log.debug("Delete {} entities size:{}, from:{}, to:{}", chunk.getItems().size(), chunk.getSize(), chunk.getFrom(), chunk.getTo());
        });
    }

    @Override
    public void deleteBatch(Collection<K> keys) {
        ChunkUtils.toChunks(this.toEntityIds(keys), DEFAULT_BATCH_DELETE).forEach(chunk -> {
            this.jpaRepository.deleteAllByIdInBatch(chunk.getItems());
            log.debug("Delete batch {} entities size:{}, from:{}, to:{}", chunk.getItems().size(), chunk.getSize(), chunk.getFrom(), chunk.getTo());
        });
    }

    @Override
    public void deleteAllInBatch() {
        this.jpaRepository.deleteAllInBatch();
    }

    @Override
    public void flush() {
        this.jpaRepository.flush();
    }
}

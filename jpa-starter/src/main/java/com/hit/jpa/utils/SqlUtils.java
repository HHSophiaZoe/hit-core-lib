package com.hit.jpa.utils;

import com.hit.common.pagination.PageResModel;
import com.hit.common.pagination.PageableReqModel;
import com.hit.common.pagination.PageableSearchReqModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@UtilityClass
public class SqlUtils {

    public static Pageable createPageable(PageableReqModel request) {
        if (request.getSorts() == null || request.getSorts().isEmpty()) {
            return PageRequest.of(request.getPage(), request.getPageSize());
        }
        List<Sort.Order> orders = request.getSorts().stream()
                .map(order -> {
                    if (BooleanUtils.isTrue(order.isAscending())) {
                        return Sort.Order.desc(order.getName());
                    } else {
                        return Sort.Order.asc(order.getName());
                    }
                })
                .toList();
        return PageRequest.of(request.getPage(), request.getPageSize(), Sort.by(orders));
    }

    public <E> Specification<E> createSpecificationPagination(PageableReqModel request, Class<E> entityClass, Collection<String> columnAccess) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            List<Predicate> predicateFilters = SqlPredicateUtils.createPredicateFilters(request.getFilters(), entityClass, root, criteriaBuilder, columnAccess);
            if (!CollectionUtils.isEmpty(predicateFilters)) {
                Predicate andPredicate = criteriaBuilder.and(predicateFilters.toArray(new Predicate[0]));
                predicates.add(andPredicate);
            }

            List<Predicate> orPredicateFilters = SqlPredicateUtils.createPredicateFilters(request.getOrFilters(), entityClass, root, criteriaBuilder, columnAccess);
            if (!CollectionUtils.isEmpty(orPredicateFilters)) {
                Predicate orPredicate = criteriaBuilder.or(orPredicateFilters.toArray(new Predicate[0]));
                predicates.add(orPredicate);
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            } else {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    public <E> Specification<E> createSpecificationPaginationSearch(PageableSearchReqModel request, Class<E> entityClass, Collection<String> columnAccess) {
        return (root, query, criteriaBuilder) -> {
            // add condition base from PaginationRequest
            Specification<E> baseSpec = createSpecificationPagination(request, entityClass, columnAccess);
            Predicate basePredicate = baseSpec.toPredicate(root, query, criteriaBuilder);

            // generate search predicate if contain contains query
            Predicate searchPredicate = Optional.ofNullable(request.getSearches())
                    .map(searches -> {
                        List<Predicate> predicateSearches = SqlPredicateUtils.createPredicateSearches(searches, request.getKeyword(), entityClass, root, criteriaBuilder, columnAccess);
                        return criteriaBuilder.or(predicateSearches.toArray(new Predicate[0]));
                    })
                    .orElseGet(criteriaBuilder::conjunction);
            return criteriaBuilder.and(basePredicate, searchPredicate);
        };
    }

    public static <T> PageResModel.PagingMeta buildPagingMeta(PageableReqModel request, Page<T> pages) {
        return new PageResModel.PagingMeta(
                pages.getTotalElements(), pages.getTotalPages(),
                request.getPage(), request.getPageSize(),
                isLoadMoreAble(pages.getTotalElements(), request.getPageSize(), request.getPageSize())
        );
    }

    @SuppressWarnings("unchecked")
    public  <T, ID> ID getEntityId(EntityManager entityManager, T entity) {
        return (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
    }

    private static boolean isLoadMoreAble(Long total, Integer pageSize, Integer offset) {
        return total != null && (total > (pageSize + offset));
    }

}

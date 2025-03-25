package com.hit.jpa.utils;

import com.hit.coremodel.pagination.PaginationRequest;
import com.hit.coremodel.pagination.PaginationSearchRequest;
import com.hit.coremodel.pagination.PagingMeta;
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

import java.util.List;
import java.util.Optional;

@Slf4j
@UtilityClass
public class SqlUtils {

    public static Pageable createPageable(PaginationRequest request) {
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


    public <E> Specification<E> createSpecificationPagination(PaginationRequest request, Class<E> entityClass) {
        return (root, query, criteriaBuilder) -> {
            // generate filter predicate if contains filter query
            return Optional.ofNullable(request.getFilters())
                    .map(filters -> {
                        List<Predicate> predicateFilters = SqlPredicateUtils.createPredicateFilters(filters, entityClass, root, criteriaBuilder);
                        return criteriaBuilder.and(predicateFilters.toArray(new Predicate[0]));
                    })
                    .orElseGet(criteriaBuilder::conjunction);
        };
    }

    public <E> Specification<E> createSpecificationPaginationSearch(PaginationSearchRequest request, Class<E> entityClass) {
        return (root, query, criteriaBuilder) -> {
            // add condition base from PaginationRequest
            Specification<E> baseSpec = createSpecificationPagination(request, entityClass);
            Predicate basePredicate = baseSpec.toPredicate(root, query, criteriaBuilder);

            // generate search predicate if contain contains query
            Predicate searchPredicate = Optional.ofNullable(request.getSearches())
                    .map(searches -> {
                        List<Predicate> predicateSearches = SqlPredicateUtils.createPredicateSearches(searches, request.getKeyword(), entityClass, root, criteriaBuilder);
                        return criteriaBuilder.or(predicateSearches.toArray(new Predicate[0]));
                    })
                    .orElseGet(criteriaBuilder::conjunction);
            return criteriaBuilder.and(basePredicate, searchPredicate);
        };
    }

    public static <T> PagingMeta buildPagingMeta(PaginationRequest request, Page<T> pages) {
        return new PagingMeta(
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

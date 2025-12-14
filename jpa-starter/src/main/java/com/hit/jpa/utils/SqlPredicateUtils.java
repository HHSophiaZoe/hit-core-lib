package com.hit.jpa.utils;

import com.hit.common.model.query.Filter;
import com.hit.common.model.query.Operator;
import com.hit.common.model.query.Search;
import com.hit.common.model.query.SearchOption;
import com.hit.jpa.exception.QueryException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class SqlPredicateUtils {

    public static <E> List<Predicate> createPredicateFilters(List<Filter> filters, Class<E> entityClass,
                                                             Root<E> root, CriteriaBuilder cb, Collection<String> columnAccess) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }
        return filters.stream()
                .map(filter -> createPredicateFilter(filter, entityClass, root, cb, columnAccess))
                .filter(Objects::nonNull)
                .toList();
    }

    public static <E> Predicate createPredicateFilter(Filter filter, Class<E> entityClass, Root<E> root,
                                                      CriteriaBuilder cb, Collection<String> columnAccess) {
        String columnName = filter.getName();
        String value = filter.getValue();
        Operator operator = Operator.fromOperator(filter.getOperator());
        try {
            if (!columnAccess.contains(columnName)) {
                log.warn("[Filter] Cannot find path from column access: {}", columnName);
                return null;
            }

            Field field = SqlTransferUtils.findField(entityClass, columnName);
            if (field == null) throw new QueryException("Invalid column: " + columnName);

            Class<?> columnType = field.getType();
            Object convertedValue = convertFilterValue(operator, value, columnType);
            if (convertedValue == null) {
                return cb.isNull(root.get(columnName));
            }

            return switch (operator) {
                case EQUAL -> cb.equal(root.get(columnName), convertedValue);
                case NOT_EQUAL -> cb.notEqual(root.get(columnName), convertedValue);
                case IN, NIN -> createPredicateInOperator(root, cb, columnName, convertedValue, operator == Operator.NIN);
                case LIKE -> createPredicateLikeOperator(root, cb, columnName, convertedValue);
                case GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL ->
                        createPredicateComparisonOperator(root, cb, columnName, convertedValue, operator);
                case NULL -> cb.isNull(root.get(columnName));
                case NOT_NULL -> cb.isNotNull(root.get(columnName));
                default -> null;
            };
        } catch (Exception e) {
            throw new QueryException("Error processing filter for column: " + columnName, e);
        }
    }

    public static <E> List<Predicate> createPredicateSearches(List<Search> searches, String keyword, Class<E> entityClass,
                                                              Root<E> root, CriteriaBuilder cb, Collection<String> columnAccess) {
        return searches.stream()
                .map(search -> createPredicateSearch(search, keyword, entityClass, root, cb, columnAccess))
                .filter(Objects::nonNull)
                .toList();
    }

    public static <E> Predicate createPredicateSearch(Search search, String keyword, Class<E> entityClass, Root<E> root,
                                                      CriteriaBuilder cb, Collection<String> columnAccess) {
        String columnName = search.getName();
        try {
            if (!columnAccess.contains(columnName)) {
                log.warn("[Search] Cannot find path from column access: {}", columnName);
                return null;
            }

            Field field = SqlTransferUtils.findField(entityClass, columnName);
            if (field == null) throw new QueryException("Invalid column: " + columnName);
            if (field.getType() != String.class) {
                throw new QueryException("Search field " + search.getName() + " invalid !!!");
            }

            SearchOption option = SearchOption.fromOption(search.getOption());
            return switch (option) {
                case EQUAL -> cb.equal(root.get(columnName), keyword);
                case LIKE -> cb.like(root.get(columnName), "%" + keyword + "%");
                case LIKE_REGEX -> throw new UnsupportedOperationException("Unsupported operation LIKE_REGEX");
                case LIKE_IGNORE_CASE -> cb.like(cb.lower(root.get(columnName)), "%" + keyword.toLowerCase() + "%");
                case LIKE_IGNORE_ACCENT -> throw new UnsupportedOperationException("Unsupported operation LIKE_IGNORE_ACCENT");
                case LIKE_IGNORE_CASE_AND_ACCENT -> throw new UnsupportedOperationException("Unsupported operation LIKE_IGNORE_CASE_AND_ACCENT");
            };
        } catch (Exception e) {
            throw new QueryException("Error processing filter for column: " + columnName, e);
        }
    }

    public static Object convertFilterValue(Operator operator, String value, Class<?> columnType) {
        if (Operator.operatorFilterListValue().contains(operator.getValue())) {
            String[] values = value.split(",");
            if (values.length == 0) return null;
            return Stream.of(values)
                    .map(object -> SqlTransferUtils.castValueByClass(object, columnType))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            return SqlTransferUtils.castValueByClass(value, columnType);
        }
    }

    private static <E> Predicate createPredicateInOperator(Root<E> root, CriteriaBuilder cb,
                                                           String columnName, Object value, boolean negate) {
        if (!(value instanceof List<?> values) || values.isEmpty()) {
            return cb.disjunction();
        }
        CriteriaBuilder.In<Object> inClause = cb.in(root.get(columnName));
        for (Object val : values) {
            inClause.value(val);
        }
        return negate ? cb.not(inClause) : inClause;
    }

    private static Predicate createPredicateLikeOperator(Root<?> root, CriteriaBuilder cb, String columnName, Object value) {
        if (String.class.equals(value.getClass())) {
            return cb.like(root.get(columnName), "%" + value + "%");
        }
        throw new QueryException(String.format("Operator 'like' not supported for data type %s", value.getClass()));
    }


    /**
     * Comparison operator: GT, LT, GTE, LTE
     */
    private static <E, T extends Comparable<T>> Predicate
    createPredicateComparisonOperator(Root<E> root, CriteriaBuilder cb, String columnName, Object value, Operator operator) {
        if (!(value instanceof Comparable<?>)) {
            throw new QueryException(String.format("Operator '%s' not supported for data type %s", operator, value.getClass()));
        }

        @SuppressWarnings("unchecked")
        T comparableValue = (T) value;
        Path<T> path = root.get(columnName);
        return switch (operator) {
            case GREATER_THAN -> cb.greaterThan(path, comparableValue);
            case LESS_THAN -> cb.lessThan(path, comparableValue);
            case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(path, comparableValue);
            case LESS_THAN_OR_EQUAL -> cb.lessThanOrEqualTo(path, comparableValue);
            default -> throw new QueryException("Comparison unsupported operator: " + operator);
        };
    }

}

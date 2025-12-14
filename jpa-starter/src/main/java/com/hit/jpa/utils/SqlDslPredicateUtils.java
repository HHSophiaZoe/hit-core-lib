package com.hit.jpa.utils;

import com.hit.common.model.pagination.PageableReqModel;
import com.hit.common.model.pagination.PageableSearchReqModel;
import com.hit.common.model.query.Filter;
import com.hit.common.model.query.Operator;
import com.hit.common.model.query.Search;
import com.hit.common.model.query.SearchOption;
import com.hit.jpa.exception.QueryException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@UtilityClass
@SuppressWarnings({"unchecked"})
public class SqlDslPredicateUtils {


    public static Predicate pageFilterPredicate(PageableReqModel request, Map<String, Path<?>> columnAccess, Predicate... predicate) {
        List<Predicate> conditions = new ArrayList<>(List.of(predicate));
        conditions.add(combinePredicatesWithAnd(createPredicateFilters(request.getFilters(), columnAccess)));
        conditions.add(combinePredicatesWithOr(createPredicateFilters(request.getOrFilters(), columnAccess)));
        return combinePredicatesWithAnd(conditions);
    }

    public static Predicate pageSearchPredicate(PageableSearchReqModel request, Map<String, Path<?>> columnAccess, Predicate... predicate) {
        List<Predicate> conditions = new ArrayList<>(List.of(predicate));
        conditions.add(combinePredicatesWithAnd(createPredicateFilters(request.getFilters(), columnAccess)));
        conditions.add(combinePredicatesWithOr(createPredicateFilters(request.getOrFilters(), columnAccess)));
        conditions.addAll(createPredicateSearches(request.getSearches(), request.getKeyword(), columnAccess));
        return combinePredicatesWithAnd(conditions);
    }

    public static List<Predicate> createPredicateFilters(List<Filter> filters, Map<String, Path<?>> columnAccess) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }
        return filters.stream()
                .map(filter -> createPredicateFilter(filter, columnAccess))
                .filter(Objects::nonNull)
                .toList();
    }

    public static Predicate createPredicateFilter(Filter filter, Map<String, Path<?>> columnAccess) {
        String columnName = filter.getName();
        String value = filter.getValue();
        Operator operator = Operator.fromOperator(filter.getOperator());

        try {
            Path<?> path = columnAccess.get(columnName);
            if (path == null) {
                log.warn("[Filter] Cannot find path from column access: {}", columnName);
                return null;
            }
            Class<?> columnType = getPathType(path);
            Object convertedValue = SqlPredicateUtils.convertFilterValue(operator, value, columnType);

            return switch (operator) {
                case EQUAL -> createEqualPredicate(path, convertedValue);
                case NOT_EQUAL -> createNotEqualPredicate(path, convertedValue);
                case IN, NIN -> createInPredicate(path, convertedValue, operator == Operator.NIN);
                case LIKE -> createLikePredicate(path, convertedValue);
                case GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL ->
                        createComparisonPredicate(path, convertedValue, operator);
                case NULL -> createNullPredicate(path);
                case NOT_NULL -> createNotNullPredicate(path);
                default -> null;
            };
        } catch (Exception e) {
            throw new QueryException("Error processing filter for column: " + columnName, e);
        }
    }

    public static List<Predicate> createPredicateSearches(List<Search> searches, String keyword, Map<String, Path<?>> columnAccess) {
        return searches.stream()
                .map(search -> createPredicateSearch(search, keyword, columnAccess))
                .filter(Objects::nonNull)
                .toList();
    }

    public static Predicate createPredicateSearch(Search search, String keyword, Map<String, Path<?>> columnAccess) {
        String columnName = search.getName();
        try {
            Path<?> path = columnAccess.get(columnName);
            if (path == null) {
                log.warn("[Search] Cannot find path from column access: {}", columnName);
                return null;
            }

            if (!(path instanceof StringPath stringPath)) {
                log.warn("[Search] Column must be StringPath: {}", columnName);
                return null;
            }

            SearchOption option = SearchOption.fromOption(search.getOption());
            return switch (option) {
                case EQUAL -> stringPath.eq(keyword);
                case LIKE -> stringPath.contains(keyword);
                case LIKE_REGEX -> throw new UnsupportedOperationException("Unsupported operation LIKE_REGEX");
                case LIKE_IGNORE_CASE -> stringPath.containsIgnoreCase(keyword);
                case LIKE_IGNORE_ACCENT -> throw new UnsupportedOperationException("Unsupported operation LIKE_IGNORE_ACCENT");
                case LIKE_IGNORE_CASE_AND_ACCENT -> throw new UnsupportedOperationException("Unsupported operation LIKE_IGNORE_CASE_AND_ACCENT");
            };
        } catch (Exception e) {
            throw new QueryException("Error processing search for column: " + columnName, e);
        }
    }

    private static Class<?> getPathType(Path<?> path) {
        if (path instanceof StringPath) return String.class;
        if (path instanceof NumberPath<?> numberPath) return numberPath.getType();
        if (path instanceof BooleanPath) return Boolean.class;
        if (path instanceof DatePath<?> datePath) return datePath.getType();
        if (path instanceof DateTimePath<?> dateTimePath) return dateTimePath.getType();
        if (path instanceof EnumPath<?> enumPath) return enumPath.getType();
        if (path instanceof ComparablePath<?> comparablePath) return comparablePath.getType();
        return Object.class;
    }

    private static Predicate createEqualPredicate(Path<?> path, Object value) {
        if (path instanceof StringPath stringPath) {
            return stringPath.eq((String) value);
        } else if (path instanceof NumberPath numberPath) {
            return numberPath.eq(value);
        } else if (path instanceof BooleanPath booleanPath) {
            return booleanPath.eq((Boolean) value);
        } else if (path instanceof DatePath datePath) {
            return datePath.eq(value);
        } else if (path instanceof DateTimePath dateTimePath) {
            return dateTimePath.eq(value);
        } else if (path instanceof EnumPath enumPath) {
            return enumPath.eq(value);
        } else if (path instanceof ComparablePath comparablePath) {
            return comparablePath.eq(value);
        }
        throw new QueryException("Unsupported path type for EQUAL operation: " + path.getClass());
    }

    private static Predicate createNotEqualPredicate(Path<?> path, Object value) {
        if (path instanceof StringPath stringPath) {
            return stringPath.ne((String) value);
        } else if (path instanceof NumberPath numberPath) {
            return numberPath.ne(value);
        } else if (path instanceof BooleanPath booleanPath) {
            return booleanPath.ne((Boolean) value);
        } else if (path instanceof DatePath datePath) {
            return datePath.ne(value);
        } else if (path instanceof DateTimePath dateTimePath) {
            return dateTimePath.ne(value);
        } else if (path instanceof EnumPath enumPath) {
            return enumPath.ne(value);
        } else if (path instanceof ComparablePath comparablePath) {
            return comparablePath.ne(value);
        }
        throw new QueryException("Unsupported path type for NOT_EQUAL operation: " + path.getClass());
    }

    @SuppressWarnings("unchecked")
    private static Predicate createInPredicate(Path<?> path, Object value, boolean negate) {
        if (!(value instanceof List<?> values) || values.isEmpty()) {
            return new BooleanBuilder(); // Empty predicate
        }

        Predicate inPredicate;
        if (path instanceof StringPath stringPath) {
            inPredicate = stringPath.in((List<String>) values);
        } else if (path instanceof NumberPath numberPath) {
            inPredicate = numberPath.in(values);
        } else if (path instanceof BooleanPath booleanPath) {
            inPredicate = booleanPath.in((List<Boolean>) values);
        } else if (path instanceof DatePath datePath) {
            inPredicate = datePath.in(values);
        } else if (path instanceof DateTimePath dateTimePath) {
            inPredicate = dateTimePath.in(values);
        } else if (path instanceof EnumPath enumPath) {
            inPredicate = enumPath.in(values);
        } else if (path instanceof ComparablePath comparablePath) {
            inPredicate = comparablePath.in(values);
        } else {
            throw new QueryException("Unsupported path type for IN operation: " + path.getClass());
        }

        return negate ? inPredicate.not() : inPredicate;
    }

    private static Predicate createLikePredicate(Path<?> path, Object value) {
        if (!(path instanceof StringPath stringPath)) {
            throw new QueryException("LIKE operator only supported for String fields");
        }
        if (!(value instanceof String stringValue)) {
            throw new QueryException("LIKE operator requires String value");
        }
        return stringPath.containsIgnoreCase(stringValue);
    }

    private static Predicate createComparisonPredicate(Path<?> path, Object value, Operator operator) {
        if (!(value instanceof Comparable<?>)) {
            throw new QueryException(String.format("Operator '%s' not supported for data type %s", operator, value.getClass()));
        }

        if (!(path instanceof ComparablePath)) {
            throw new QueryException(String.format("Operator '%s' not supported for path type %s", operator, path.getClass()));
        }

        ComparablePath comparablePath = (ComparablePath) path;
        Comparable comparableValue = (Comparable) value;
        return switch (operator) {
            case GREATER_THAN -> comparablePath.gt(comparableValue);
            case LESS_THAN -> comparablePath.lt(comparableValue);
            case GREATER_THAN_OR_EQUAL -> comparablePath.goe(comparableValue);
            case LESS_THAN_OR_EQUAL -> comparablePath.loe(comparableValue);
            default -> throw new QueryException("Unsupported comparison operator: " + operator);
        };
    }


    private static Predicate createNullPredicate(Path<?> path) {
        return ((SimpleExpression<?>) path).isNull();
    }

    private static Predicate createNotNullPredicate(Path<?> path) {
        return ((SimpleExpression<?>) path).isNotNull();
    }

    /**
     * Combine multiple predicates with AND logic
     */
    public static Predicate combinePredicatesWithAnd(List<Predicate> predicates) {
        BooleanBuilder builder = new BooleanBuilder();
        predicates.forEach(builder::and);
        return builder;
    }

    /**
     * Combine multiple predicates with OR logic
     */
    public static Predicate combinePredicatesWithOr(List<Predicate> predicates) {
        BooleanBuilder builder = new BooleanBuilder();
        predicates.forEach(builder::or);
        return builder;
    }

}

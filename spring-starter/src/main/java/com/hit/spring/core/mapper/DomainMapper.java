package com.hit.spring.core.mapper;

import com.hit.spring.util.ReflectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.IterableMapping;
import org.mapstruct.Named;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public interface DomainMapper<E, M> {

    @SuppressWarnings("unchecked")
    default Pair<Class<E>, Class<M>> getClassGeneric() {
        Class<?> implementingClass = this.getClass();
        Type[] genericInterfaces = ((Class<?>) implementingClass.getGenericInterfaces()[0]).getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(DomainMapper.class)) {
                Type[] typeArguments = ((ParameterizedType) genericInterface).getActualTypeArguments();
                return Pair.of((Class<E>) typeArguments[0], (Class<M>) typeArguments[1]);
            }
        }
        throw new IllegalStateException("Could not determine generic types for " + implementingClass.getName());
    }

    /**
     * To Entity
     */
    @Named("toEntity")
    E toEntity(M m);

    @IterableMapping(qualifiedByName = "toEntity")
    List<E> toEntities(List<M> list);

    /**
     * To Model
     */
    @Named("toModel")
    M toModel(E e);

    @IterableMapping(qualifiedByName = "toModel")
    List<M> toModels(List<E> list);

    @Named("toBasicModel")
    default M toBasicModel(E e) {
        try {
            if (e == null) return null;
            M model = getClassGeneric().getRight().getDeclaredConstructor().newInstance();
            ReflectUtils.copyBasicFields(e, model);
            return model;
        } catch (Exception ex) {
            throw new IllegalStateException("toBasicModel error: " + ex.getMessage(), ex);
        }
    }

    @IterableMapping(qualifiedByName = "toBasicModel")
    List<M> toBasicModels(List<E> list);

}

package com.hit.spring.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class ReflectUtils {
//
//    @SuppressWarnings("unchecked")
//    public <T> T createModelInstance() {
//        try {
//            Class<?> clazz = Class.forName(T/);
//            Constructor<T> constructor = (Constructor<T>) clazz.getClass().getDeclaredConstructor();
//            constructor.setAccessible(true);
//            return constructor.newInstance();
//        } catch (NoSuchMethodException e) {
//            log.error("No default constructor found for {}", modelClass.getName(), e);
//            throw new RuntimeException("No default constructor available", e);
//        } catch (Exception e) {
//            log.error("Error creating instance of {}", modelClass.getName(), e);
//            throw new RuntimeException("Failed to create instance", e);
//        }
//    }

    public static Object getValueByFieldName(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            boolean isPrivate = Modifier.isPrivate(field.getModifiers());
            if (isPrivate) {
                field.setAccessible(true);
            }
            Object value = field.get(obj);
            if (isPrivate) {
                field.setAccessible(false);
            }
            return value;
        } catch (NoSuchFieldException e) {
            log.error("[field {} not found] cause: {}", fieldName, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[fail to get value field {}] cause: {}", fieldName, e.getMessage(), e);
        }
        return null;
    }

    public static Object getValueByField(Field field, Object obj) {
        try {
            boolean isPrivate = Modifier.isPrivate(field.getModifiers());
            if (isPrivate) {
                field.setAccessible(true);
            }
            Object value = field.get(obj);
            if (isPrivate) {
                field.setAccessible(false);
            }
            return value;
        }  catch (Exception e) {
            log.error("[fail to get value field {}] cause: {}", field.getName(), e.getMessage(), e);
        }
        return null;
    }

    public static Map<String, Object> getValuesByObject(Object obj) {
        try {
            Map<String, Object> result = new HashMap<>();
            for (Method method : obj.getClass().getMethods()) {
                if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                    Object value = method.invoke(obj);
                    String fieldName = method.getName().substring(3);
                    result.put(fieldName, value);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[fail to invoke method] cause: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    public static Field getFieldByName(Object obj, String fieldName) {
        try {
            return obj.getClass().getDeclaredField(fieldName);
        } catch (Exception e) {
            log.error("[fail to get field {}] cause: {}", fieldName, e.getMessage(), e);
        }
        return null;
    }

    public static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    public static Method getSetter(Class<?> clazz, String fieldName) {
        if (clazz == null || StringUtils.isEmpty(fieldName)) {
            return null;
        }
        try {
            String capitalizedFieldName = StringUtils.capitalize(fieldName);
            Class<?> paramType;
            try {
                Method getter = clazz.getDeclaredMethod("get" + capitalizedFieldName);
                paramType = getter.getReturnType();
            } catch (NoSuchMethodException e) {
                Method isGetter = clazz.getDeclaredMethod("is" + capitalizedFieldName);
                paramType = isGetter.getReturnType();
            }
            return clazz.getDeclaredMethod("set" + capitalizedFieldName, paramType);
        } catch (Exception e) {
            if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)) {
                return getSetter(clazz.getSuperclass(), fieldName);
            }
            log.error("[fail to get setter field {}] cause: {}", fieldName, e.getMessage(), e);
        }
        return null;
    }

    public static Method getGetter(Class<?> clazz, String fieldName) {
        if (clazz == null || StringUtils.isEmpty(fieldName)) {
            return null;
        }
        try {
            String capitalizedFieldName = StringUtils.capitalize(fieldName);
            try {
                return clazz.getDeclaredMethod("get" + capitalizedFieldName);
            } catch (NoSuchMethodException e) {
                return clazz.getDeclaredMethod("is" + capitalizedFieldName);
            }
        } catch (Exception e) {
            if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)) {
                return getSetter(clazz.getSuperclass(), fieldName);
            }
            log.error("[fail to get getter field {}] cause: {}", fieldName, e.getMessage(), e);
        }
        return null;
    }

    public static Object invokeMethodByName(Object obj, String methodName, Object... args) {
        java.lang.reflect.Method method;
        try {
            method = obj.getClass().getMethod(methodName);
            if (args == null || args.length == 0)
                return method.invoke(obj);
            return method.invoke(obj, args);
        } catch (SecurityException | NoSuchMethodException e) {
            log.error("[method {} not found] cause: {}", methodName, e.getMessage(), e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("[fail to invoke method] cause: {}", e.getMessage(), e);
        }
        return null;
    }

    public static Object invokeMethodByName(Object obj, String methodName, Class<?>[] paramsTypes, Object[] args) {
        java.lang.reflect.Method method;
        try {
            method = obj.getClass().getMethod(methodName, paramsTypes);
            return method.invoke(obj, args);
        } catch (SecurityException | NoSuchMethodException e) {
            log.error("[method {} not found] cause: {}", methodName, e.getMessage(), e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("[fail to invoke method] cause: {}", e.getMessage(), e);
        }
        return null;
    }

    public static void invokeSetMethodByFieldName(Object obj, String fieldName, Class<?> paramType, Object arg) {
        String methodName = "set" + StringUtils.capitalize(fieldName);
        Class<?>[] paramTypes = {paramType};
        Object[] args = {arg};
        invokeMethodByName(obj, methodName, paramTypes, args);
    }

    public static <T, A extends Annotation> Optional<A> getAnnotationInClass(Class<T> classFind, Class<A> annotationType) {
        for (Annotation annotation : classFind.getAnnotations()) {
            if (annotationType.equals(annotation.annotationType())) {
                return Optional.of(annotationType.cast(annotation));
            }
        }
        return Optional.empty();
    }

    public static <T, A extends Annotation> Map<String, A> getMapAnnotationInFields(Class<T> classFind, Class<A> annotationType) {
        Map<String, A> annotationMap = new HashMap<>();
        for (Field field : classFind.getDeclaredFields()) {
            A fieldAnnotation = field.getAnnotation(annotationType);
            if (fieldAnnotation != null) {
                annotationMap.put(field.getName(), fieldAnnotation);
            }
        }
        return annotationMap;
    }

    public static <T, A extends Annotation> List<A> getListAnnotationInFields(Class<T> classFind, Class<A> annotationType) {
        List<A> annotations = new ArrayList<>();
        for (Field field : classFind.getDeclaredFields()) {
            A fieldAnnotation = field.getAnnotation(annotationType);
            if (fieldAnnotation != null) {
                annotations.add(fieldAnnotation);
            }
        }
        return annotations;
    }

    public static <T> List<Annotation> getMapAnnotationInFields(Class<T> classFind) {
        List<Annotation> annotations = new ArrayList<>();
        for (Field field : classFind.getDeclaredFields()) {
            annotations.addAll(Arrays.stream(field.getAnnotations()).toList());
        }
        return annotations;
    }

    public static <E, M> void copyBasicFields(E source, M target) throws InvocationTargetException, IllegalAccessException {
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();
        Field[] declaredTargetFields = targetClass.getDeclaredFields();
        for (Field targetField : declaredTargetFields) {
            if (!isBasicType(targetField.getType())) {
                log.debug("skip copy not basic field {}", targetField.getName());
                continue;
            }
            try {
                String fieldName = targetField.getName();
                Method sourceGetter = getGetter(sourceClass, fieldName);
                Method targetSetter = getSetter(targetClass, fieldName);
                if (sourceGetter != null && targetSetter != null) {
                    Object sourceValue = sourceGetter.invoke(source);
                    if (sourceValue != null && isCompatibleType(targetSetter.getParameterTypes()[0], sourceValue.getClass())) {
                        targetSetter.invoke(target, sourceValue);
                    }
                }
            } catch (Exception ex) {
                log.debug("Failed to copy field '{}': {}", targetField.getName(), ex.getMessage());
                throw ex;
            }
        }
    }

    private boolean isBasicType(Class<?> type) {
        return type.isPrimitive() ||
                type == String.class ||
                Number.class.isAssignableFrom(type) ||
                type == Boolean.class ||
                Date.class.isAssignableFrom(type) ||
                TemporalAccessor.class.isAssignableFrom(type);
    }

    private boolean isCompatibleType(Class<?> sourceType, Class<?> targetType) {
        if (sourceType.isPrimitive()) {
            return targetType == getWrapperType(sourceType);
        }
        if (targetType.isPrimitive()) {
            return sourceType == getWrapperType(targetType);
        }
        return targetType.isAssignableFrom(sourceType) ||
                (Number.class.isAssignableFrom(targetType) && Number.class.isAssignableFrom(sourceType));
    }

    private Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) return Integer.class;
        if (primitiveType == long.class) return Long.class;
        if (primitiveType == double.class) return Double.class;
        if (primitiveType == float.class) return Float.class;
        if (primitiveType == boolean.class) return Boolean.class;
        if (primitiveType == char.class) return Character.class;
        if (primitiveType == byte.class) return Byte.class;
        if (primitiveType == short.class) return Short.class;
        return primitiveType;
    }
}

package com.hit.spring.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class ReflectUtils {

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
            log.error("[field " + fieldName + "not found] cause: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[fail to get value field " + fieldName + "] cause: {}", e.getMessage(), e);
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
            log.error("[fail to get value field " + field.getName() + "] cause: {}", e.getMessage(), e);
        }
        return null;
    }

    public static Map<String, Object> getValueFromObject(Object obj) {
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
            log.error("[fail to get field " + fieldName + "] cause: {}", e.getMessage(), e);
        }
        return null;
    }

    public static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    public static Object invokeMethodByName(Object obj, String methodName, Object... args) {
        java.lang.reflect.Method method;
        try {
            method = obj.getClass().getMethod(methodName);
            if (args == null || args.length == 0)
                return method.invoke(obj);
            return method.invoke(obj, args);
        } catch (SecurityException | NoSuchMethodException e) {
            log.error("[method " + methodName + "not found] cause: {}", e.getMessage(), e);
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
            log.error("[method " + methodName + "not found] cause: {}", e.getMessage(), e);
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
}

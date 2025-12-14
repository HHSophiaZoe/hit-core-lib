package com.hit.spring.core.converter;

import com.hit.common.util.NumberUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class DataConverter {

    @Setter(onMethod_ = {@Autowired(required = false), @Qualifier("mvcConversionService")})
    private ConversionService conversionService;

    private static final Map<Class<?>, Function<Object, ?>> CONVERTERS = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) return null;

        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }

        // Cache converter
        Function<Object, T> converter = (Function<Object, T>) CONVERTERS.computeIfAbsent(
                targetType, k -> this.createConverter(targetType)
        );
        return converter.apply(value);
    }

    @SuppressWarnings({"java:S3776"})
    private Function<Object, ?> createConverter(Class<?> targetType) {
        // Implement specific conversion logic
        if (Integer.class.equals(targetType)) {
            return val -> NumberUtils.safeParseInteger(val.toString());
        }
        if (int.class.equals(targetType)) {
            return val -> NumberUtils.safeParseInteger(val.toString(), 0);
        }

        if (Long.class.equals(targetType)) {
            return val -> NumberUtils.safeParseLong(val.toString());
        }
        if (long.class.equals(targetType)) {
            return val -> NumberUtils.safeParseLong(val.toString(), 0L);
        }

        if (Float.class.equals(targetType)) {
            return val -> NumberUtils.safeParseFloat(val.toString());
        }
        if (float.class.equals(targetType)) {
            return val -> NumberUtils.safeParseFloat(val.toString(), 0f);
        }

        if (Double.class.equals(targetType)) {
            return val -> NumberUtils.safeParseDouble(val.toString());
        }
        if (double.class.equals(targetType)) {
            return val -> NumberUtils.safeParseDouble(val.toString(), 0d);
        }

        if (Short.class.equals(targetType)) {
            return val -> NumberUtils.safeParseShort(val.toString());
        }
        if (short.class.equals(targetType)) {
            return val -> NumberUtils.safeParseShort(val.toString(), 0);
        }

        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
            return val -> BooleanUtils.toBooleanObject(val.toString());
        }

        return val -> {
            log.debug("Fallback to reflection conversion service");
            if (ObjectUtils.isEmpty(conversionService)) {
                log.error("conversionService don't injection");
                return null;
            }
            return conversionService.convert(val, targetType);
        };
    }

}

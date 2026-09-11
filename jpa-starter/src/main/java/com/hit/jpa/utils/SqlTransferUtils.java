package com.hit.jpa.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;

@Slf4j
@UtilityClass
public class SqlTransferUtils {

    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    public static Object castValueByClass(String value, Class<?> fieldType) {
        try {
            Object convertedValue;
            if (String.class.equals(fieldType)) {
                return value;
            } else if (isNumberType(fieldType)) {
                convertedValue = parseNumber(value, fieldType);
            } else if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
                String normalizedValue = value == null ? null : value.trim();
                if (!"true".equalsIgnoreCase(normalizedValue) && !"false".equalsIgnoreCase(normalizedValue)) {
                    throw new IllegalArgumentException("Invalid boolean value: " + value);
                }
                convertedValue = Boolean.parseBoolean(normalizedValue);
            } else if (Date.class.isAssignableFrom(fieldType) || Temporal.class.isAssignableFrom(fieldType)) {
                convertedValue = parseDateTime(value, fieldType);
            } else if (fieldType.isEnum()) {
                convertedValue = parseEnum(value, fieldType);
            } else {
                return value;
            }

            if (convertedValue == null) {
                throw new IllegalArgumentException("Cannot convert value '%s' to %s".formatted(value, fieldType.getSimpleName()));
            }
            return convertedValue;
        } catch (Exception e) {
            log.debug("Cannot convert value '{}' to {}", value, fieldType.getName());
            throw new IllegalArgumentException("Cannot convert value '%s' to %s".formatted(value, fieldType.getSimpleName()), e);
        }
    }

    private boolean isNumberType(Class<?> fieldType) {
        return Number.class.isAssignableFrom(fieldType)
                || fieldType == byte.class
                || fieldType == short.class
                || fieldType == int.class
                || fieldType == long.class
                || fieldType == float.class
                || fieldType == double.class;
    }

    public static Number parseNumber(String value, Class<?> fieldType) {
        try {
            if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
                return Integer.parseInt(value);
            } else if (Long.class.equals(fieldType) || long.class.equals(fieldType)) {
                return Long.parseLong(value);
            } else if (Double.class.equals(fieldType) || double.class.equals(fieldType)) {
                return Double.parseDouble(value);
            } else if (Float.class.equals(fieldType) || float.class.equals(fieldType)) {
                return Float.parseFloat(value);
            } else if (Short.class.equals(fieldType) || short.class.equals(fieldType)) {
                return Short.parseShort(value);
            } else if (Byte.class.equals(fieldType) || byte.class.equals(fieldType)) {
                return Byte.parseByte(value);
            } else if (BigDecimal.class.equals(fieldType)) {
                return new BigDecimal(value);
            } else if (BigInteger.class.equals(fieldType)) {
                return new BigInteger(value);
            }
        } catch (NumberFormatException e) {
            log.error("Error parsing number value '{}' to {}: {}", value, fieldType.getName(), e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object parseEnum(String value, Class<?> enumClass) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Enum.valueOf((Class<Enum>) enumClass, value.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value: {} for enum: {}", value, enumClass.getName());
            return null;
        }
    }

    public static Object parseDateTime(String value, Class<?> fieldType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            // LocalDateTime và các lớp liên quan
            if (java.time.temporal.Temporal.class.isAssignableFrom(fieldType)) {
                return parseTemporalType(value, fieldType);
            }
            // java.util.Date và các lớp con
            else if (java.util.Date.class.isAssignableFrom(fieldType)) {
                return parseLegacyDateType(value, fieldType);
            }
        } catch (Exception e) {
            log.debug("Cannot parse date value '{}' as {}", value, fieldType.getName());
        }
        return null;
    }

    private Object parseTemporalType(String value, Class<?> fieldType) {
        if (Instant.class.equals(fieldType)) {
            return parseInstant(value);
        }
        return parseTemporalTypeSafe(value, fieldType);
    }

    private Object parseTemporalTypeSafe(String value, Class<?> fieldType) {
        if (LocalDateTime.class.equals(fieldType)) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (OffsetDateTime.class.equals(fieldType)) {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else if (ZonedDateTime.class.equals(fieldType)) {
            return ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } else if (LocalDate.class.equals(fieldType)) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (LocalTime.class.equals(fieldType)) {
            return LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME);
        }
        return null;
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeException ignored) {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        }
    }

    private Object parseLegacyDateType(String value, Class<?> fieldType) {
        if (java.sql.Date.class.equals(fieldType)) {
            return java.sql.Date.valueOf(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (Time.class.equals(fieldType)) {
            return Time.valueOf(LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME));
        }

        Instant instant;
        try {
            instant = Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            instant = parseInstant(value);
        }
        return convertInstantToTargetDateTime(instant, fieldType);
    }

    private Object convertInstantToTargetDateTime(Instant instant, Class<?> fieldType) {
        if (Instant.class.equals(fieldType)) return instant;
        if (Date.class.equals(fieldType)) return new Date(instant.toEpochMilli());
        if (Timestamp.class.equals(fieldType)) return Timestamp.from(instant);
        if (Long.class.equals(fieldType)) return instant.toEpochMilli();
        return null;
    }

}

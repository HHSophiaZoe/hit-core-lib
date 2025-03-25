package com.hit.jpa.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Stream;

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
            if (String.class.equals(fieldType)) {
                return value;
            } else if (Number.class.isAssignableFrom(fieldType)) {
                return parseNumber(value, fieldType);
            } else if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
                return Boolean.parseBoolean(value);
            } else if (Date.class.isAssignableFrom(fieldType) || Temporal.class.isAssignableFrom(fieldType)) {
                return parseDateTime(value, fieldType);
            }
            return value;
        } catch (Exception e) {
            log.error("Error converting value '" + value + "' to " + fieldType.getName(), e);
            return null;
        }
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
            log.error("Error parsing number value '" + value + "' to " + fieldType.getName() + ": " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(parseDateTime("2025-03-20T14:30:00.123+07:00", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00.123488885+07:00", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00.123Z", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00.12345886Z", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00+07:00", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00Z", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00Z", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00Z", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:00Z", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T14:30:01", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-20T15:05:47.337067400", LocalDateTime.class));
        System.out.println(parseDateTime(new Date().toString(), LocalDateTime.class));
        System.out.println(parseDateTime(String.valueOf(new Date().getTime()), LocalDateTime.class));
        System.out.println(parseDateTime("2024-03-15 14:30:00 +0700", LocalDateTime.class));
        System.out.println(parseDateTime("15/Feb/2024 14:30:00 +0700", LocalDateTime.class));

        System.out.println(parseDateTime(LocalDate.now().toString(), LocalDate.class));
        System.out.println(parseDateTime("21/03/2025 14:30", LocalDate.class));
        System.out.println(parseDateTime(LocalTime.now().toString(), LocalTime.class));

        System.out.println(parseDateTime("2025-03-21 14:30", LocalDateTime.class));
        System.out.println(parseDateTime("2025-03-21 14:30:45", LocalDateTime.class));
        System.out.println(parseDateTime("2025/03/21 14:30", LocalDateTime.class));
        System.out.println(parseDateTime("2025/03/21 14:30:45", Date.class));
        System.out.println(parseDateTime("21-03-2025 14:30:45", LocalDateTime.class));
        System.out.println(parseDateTime("21-03-2025 14:30", Date.class));
        System.out.println(parseDateTime("21/03/2025 14:30:45", LocalDateTime.class));
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
            log.error("Error parsing date value '{}' to ", fieldType.getName(), e);
        }
        return null;
    }

    private static final String[] BASIC_DATE_TIME_PATTERNS = {
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
    };

    private static final String[] LEGACY_DATE_TIME_PATTERNS = {
            "EEE MMM dd HH:mm:ss zzz yyyy", // Example: Wed Jul 04 15:45:23 ICT 2023
            "yyyy-MM-dd HH:mm:ss Z", // Example: 2024-03-15 14:30:00 +0700
            "dd/MMM/yyyy HH:mm:ss Z" // Example: 15/Feb/2024 14:30:00 +0700
    };

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm")
    };

    private static final DateTimeFormatter[] FLEXIBLE_FORMATTERS;


    static {
        DateTimeFormatter isoLocalFormatter = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .optionalStart().appendOffsetId().optionalEnd()
                .toFormatter();
        DateTimeFormatter[] basicDateTimeFormatters = Stream.of(BASIC_DATE_TIME_PATTERNS)
                .map(DateTimeFormatter::ofPattern).toArray(DateTimeFormatter[]::new);

        DateTimeFormatter[] combinedBasicFormatters = new DateTimeFormatter[basicDateTimeFormatters.length + 1];
        combinedBasicFormatters[0] = isoLocalFormatter;
        System.arraycopy(basicDateTimeFormatters, 0, combinedBasicFormatters, 1, basicDateTimeFormatters.length);

        DateTimeFormatter[] legacyDateTimeFormatters = Stream.of(LEGACY_DATE_TIME_PATTERNS)
                .map(DateTimeFormatter::ofPattern).toArray(DateTimeFormatter[]::new);
        FLEXIBLE_FORMATTERS = ArrayUtils.addAll(combinedBasicFormatters, legacyDateTimeFormatters);
    }

    private Object parseTemporalType(String value, Class<?> fieldType) {
        try {
            long epochMilli = Long.parseLong(value);
            Instant instant = Instant.ofEpochMilli(epochMilli);
            return convertInstantToTargetDateTime(instant, fieldType);
        } catch (NumberFormatException e) {
            return parseTemporalTypeSafe(value, fieldType);
        }
    }

    private Object parseTemporalTypeSafe(String value, Class<?> fieldType) {
        if (LocalDateTime.class.equals(fieldType)) {
            return parseDateTime(value, FLEXIBLE_FORMATTERS, LocalDateTime::from);
        } else if (OffsetDateTime.class.equals(fieldType)) {
            return parseDateTime(value, FLEXIBLE_FORMATTERS, OffsetDateTime::from);
        } else if (ZonedDateTime.class.equals(fieldType)) {
            return parseDateTime(value, FLEXIBLE_FORMATTERS, ZonedDateTime::from);
        } else if (LocalDate.class.equals(fieldType)) {
            return parseDateTime(value, DATE_FORMATTERS, LocalDate::from);
        } else if (LocalTime.class.equals(fieldType)) {
            return parseDateTime(value, TIME_FORMATTERS, LocalTime::from);
        } else if (Long.class.equals(fieldType)) {
            LocalDateTime dateTime = parseDateTime(value, FLEXIBLE_FORMATTERS, LocalDateTime::from);
            if (dateTime != null) {
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        }
        return null;
    }

    private Object parseLegacyDateType(String value, Class<?> fieldType) {
        TemporalAccessor temporal = parseDateTime(value, FLEXIBLE_FORMATTERS, Function.identity());
        if (temporal == null) return null;

        Instant instant;
        if (temporal.isSupported(ChronoField.YEAR) && temporal.isSupported(ChronoField.MONTH_OF_YEAR) && temporal.isSupported(ChronoField.DAY_OF_MONTH)) {
            LocalDate localDate = LocalDate.from(temporal);
            instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } else {
            instant = Instant.from(temporal);
        }
        return convertInstantToTargetDateTime(instant, fieldType);
    }

    private <T> T parseDateTime(String value, DateTimeFormatter[] formatters, Function<TemporalAccessor, T> converter) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                TemporalAccessor parsed = formatter.parse(value);
                return converter.apply(parsed);
            } catch (Exception e) {
                // Skip
            }
        }
        return null;
    }

    private Object convertInstantToTargetDateTime(Instant instant, Class<?> fieldType) {
        if (Date.class.equals(fieldType)) return new Date(instant.toEpochMilli());
        if (java.sql.Date.class.equals(fieldType)) return new java.sql.Date(instant.toEpochMilli());
        if (Timestamp.class.equals(fieldType)) return new Timestamp(instant.toEpochMilli());
        if (Time.class.equals(fieldType)) return new Time(instant.toEpochMilli());
        if (Long.class.equals(fieldType)) return instant.toEpochMilli();
        if (LocalDateTime.class.equals(fieldType)) return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        if (LocalDate.class.equals(fieldType)) return LocalDate.ofInstant(instant, ZoneId.systemDefault());
        if (ZonedDateTime.class.equals(fieldType)) return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        if (OffsetDateTime.class.equals(fieldType)) return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
        return null;
    }

}

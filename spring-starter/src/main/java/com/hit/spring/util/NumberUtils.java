package com.hit.spring.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@UtilityClass
public class NumberUtils {

    public static Integer safeParseInteger(String value) {
        return safeParseInteger(value, null);
    }

    public static Integer safeParseInteger(String value, Integer defaultValue) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.error("safeParseInteger err", e);
            return defaultValue;
        }
    }

    public static Long safeParseLong(String value) {
        return safeParseLong(value, null);
    }

    public static Long safeParseLong(String value, Long defaultValue) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            log.error("safeParseLong err", e);
            return defaultValue;
        }
    }

    public static Double safeParseDouble(String value) {
        return safeParseDouble(value, null);
    }

    public static Double safeParseDouble(String value, Double defaultValue) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            log.error("parseDouble err", e);
            return defaultValue;
        }
    }

    public static Float safeParseFloat(String value) {
        return safeParseFloat(value, null);
    }

    public static Float safeParseFloat(String value, Float defaultValue) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            log.error("safeParseFloat err", e);
            return defaultValue;
        }
    }

    public static Short safeParseShort(String value) {
        return safeParseShort(value, 0);
    }

    public static Short safeParseShort(String value, int defaultValue) {
        if (StringUtils.isEmpty(value)) {
            return (short) defaultValue;
        }
        try {
            return Short.parseShort(value);
        } catch (Exception e) {
            log.error("safeParseShort err", e);
            return (short) defaultValue;
        }
    }

    public static Double round(Double value) {
        return round(value, 2);
    }

    public static Double round(Double value, int scale) {
        if (value == null) return null;

        return new BigDecimal(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

}

package com.hit.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@UtilityClass
public class NumberUtils {

    private static final int MONEY_SCALE = 8;
    private static final int RATIO_SCALE = 16;

    public static BigDecimal divideMoney(BigDecimal amount, int divisor) {
        return amount.divide(BigDecimal.valueOf(divisor), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static double ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() <= 0) return 0D;
        return numerator.divide(denominator, RATIO_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    public static boolean isPositiveFinite(Number value) {
        if (value == null) return false;

        double number = value.doubleValue();
        return Double.isFinite(number) && number > 0;
    }

    public static boolean isNegativeOrNonFinite(Number value) {
        if (value == null) return true;

        double number = value.doubleValue();
        return !Double.isFinite(number) || number < 0;
    }

    public static Integer nullToZero(Integer val){
        return val == null ? 0 : val;
    }

    public static Double nullToZero(Double val){
        return val == null ? 0d : val;
    }

    public static Double toDouble(Integer val) {
        return val == null ? null : val.doubleValue();
    }

    public static Double toDoubleOrZero(Integer val) {
        return val == null ? 0d : val.doubleValue();
    }

    public static Double toDouble(Long val) {
        return val == null ? null : val.doubleValue();
    }

    public static Double toDoubleOrZero(Long val) {
        return val == null ? 0d : val.doubleValue();
    }

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
            log.warn("safeParseInteger err", e);
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
            log.warn("safeParseLong err", e);
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
            log.warn("parseDouble err", e);
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
            log.warn("safeParseFloat err", e);
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
            log.warn("safeParseShort err", e);
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

    public static String formatRound(Double value) {
        return formatRound(value, 2);
    }

    public static String formatRound(Double value, int scale) {
        if (value == null) return null;

        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static Double safeRound(Double value) {
        return safeRound(value, 2);
    }

    public static Double safeRound(Double value, int scale) {
        if (value == null) return 0.0;

        return new BigDecimal(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

}

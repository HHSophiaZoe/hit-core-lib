package com.hit.spring.utils;

import com.hit.spring.core.json.JsonMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static com.hit.spring.core.constants.CommonConstant.EMPTY_STRING;

@Slf4j
@UtilityClass
public class DataUtils {

    private final SecureRandom random = new SecureRandom();

    public static String parserLog(Object data) {
        try {
            return JsonMapper.encode(data);
        } catch (Exception e) {
            return EMPTY_STRING;
        }
    }

    public String generateOTP(Integer length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10)); // 0-9
        }
        return otp.toString();
    }

    public static String formatCurrencyVND(BigInteger amount) {
        Locale locale = Locale.forLanguageTag("vi-VN");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setDecimalSeparator('.');
        DecimalFormat currencyFormatter = new DecimalFormat("###,###,###", symbols);
        return currencyFormatter.format(amount);
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

    public static String safeToString(Object obj) {
        if (ObjectUtils.isEmpty(obj)) {
            return EMPTY_STRING;
        }
        return String.valueOf(obj);
    }
}

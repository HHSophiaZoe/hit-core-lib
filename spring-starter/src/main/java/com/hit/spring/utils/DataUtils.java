package com.hit.spring.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hit.spring.core.constants.CommonConstant;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@UtilityClass
public class DataUtils {

    private final Random random = new Random();

    public static String parserLog(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .registerModule(new JavaTimeModule())
                    .setDateFormat(new StdDateFormat().withColonInTimeZone(true))
                    .setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("parserLog error: {}", e.getMessage(), e);
            return "";
        }
    }

    public static String genCorrelationId(String appName) {
        return genCorrelationId(null, appName);
    }

    public static String genCorrelationId(byte[] correlationIdByte, String appName) {
        if (correlationIdByte != null) {
            return new String(correlationIdByte);
        }
        String uuId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        return (appName + "-" + uuId).trim();
    }

    public String generateOTP(Integer length) {
        String numbers = "0123456789";

        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < length; i++) {
            otp.append(numbers.charAt(random.nextInt(numbers.length())));
        }
        return otp.toString();
    }

    public static String formatCurrency(BigInteger amount) {
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
            return CommonConstant.EMPTY_STRING;
        }
        return String.valueOf(obj);
    }
}

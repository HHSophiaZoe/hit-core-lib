package com.hit.spring.util;

import com.hit.spring.core.constant.CommonConstant;
import com.hit.spring.core.json.JsonMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Slf4j
@UtilityClass
public class DataUtils {

    private final SecureRandom random = new SecureRandom();

    public static String parserLog(Object data) {
        try {
            return JsonMapper.encode(data);
        } catch (Exception e) {
            return CommonConstant.EMPTY_STRING;
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

}

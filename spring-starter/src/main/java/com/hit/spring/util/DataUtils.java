package com.hit.spring.util;

import com.hit.spring.core.constant.CommonConstant;
import com.hit.spring.core.json.JsonMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@UtilityClass
public class DataUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final ThreadLocal<NumberFormat> VND_FORMAT =
            ThreadLocal.withInitial(() -> NumberFormat.getInstance(Locale.of("vi", "VN")));

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
            otp.append(SECURE_RANDOM.nextInt(10)); // 0-9
        }
        return otp.toString();
    }

    public static String formatCurrencyVND(BigDecimal amount) {
        if (amount == null) return "0";
        BigDecimal value = amount.setScale(0, RoundingMode.HALF_UP);
        return VND_FORMAT.get().format(value);
    }

    public static String formatCurrencyVND(Number amount) {
        if (amount == null) return "0";
        BigDecimal value = new BigDecimal(amount.toString())
                .setScale(0, RoundingMode.HALF_UP);
        return VND_FORMAT.get().format(value);
    }

}

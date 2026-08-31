package com.hit.common.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class VndMoneyUtils {

    private static final int VND_SCALE = 0;
    private static final int CALCULATION_SCALE = 8;

    public static long round(BigDecimal value) {
        return value.setScale(VND_SCALE, RoundingMode.HALF_UP).longValueExact();
    }

    public static long round(double value) {
        return round(BigDecimal.valueOf(value));
    }

    public static long multiply(long price, int quantity) {
        return Math.multiplyExact(price, quantity);
    }

    public static long calculateRate(long amount, BigDecimal rate) {
        return round(BigDecimal.valueOf(amount).multiply(rate));
    }

    public static long applyRate(long amount, double rate) {
        return round(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(1D + rate)));
    }

    public static BigDecimal divideMoney(BigDecimal amount, int divisor) {
        return amount.divide(BigDecimal.valueOf(divisor), CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal average(long amount, int quantity) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return divideMoney(BigDecimal.valueOf(amount), quantity);
    }

    public static long roundedAverage(long amount, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("VND average quantity must be positive");
        }
        return round(average(amount, quantity));
    }

    public static long allocate(long totalAmount, int allocatedQuantity, int totalQuantity) {
        if (allocatedQuantity < 0 || totalQuantity <= 0 || allocatedQuantity > totalQuantity) {
            throw new IllegalArgumentException("Invalid VND allocation quantity");
        }
        if (allocatedQuantity == totalQuantity) {
            return totalAmount;
        }
        return round(divideMoney(
                BigDecimal.valueOf(totalAmount).multiply(BigDecimal.valueOf(allocatedQuantity)), totalQuantity));
    }
}

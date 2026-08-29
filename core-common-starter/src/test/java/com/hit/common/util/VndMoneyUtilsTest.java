package com.hit.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VndMoneyUtilsTest {

    @Test
    void shouldRoundVndUsingHalfUpPolicy() {
        assertEquals(2_700L, VndMoneyUtils.round(new BigDecimal("2700.49")));
        assertEquals(2_701L, VndMoneyUtils.round(new BigDecimal("2700.50")));
    }

    @Test
    void shouldPreserveTotalAmountWhenAllocatingTheLastQuantity() {
        long firstAllocation = VndMoneyUtils.allocate(1_000L, 1, 3);
        long remainingAllocation = VndMoneyUtils.allocate(1_000L - firstAllocation, 2, 2);

        assertEquals(1_000L, firstAllocation + remainingAllocation);
    }
}

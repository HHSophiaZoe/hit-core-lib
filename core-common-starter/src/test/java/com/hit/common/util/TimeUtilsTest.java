package com.hit.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsTest {

    @Test
    void shouldParseSupportedDatePatternsWithoutHardcodedFormatters() {
        LocalDate expected = LocalDate.of(2025, 8, 27);

        assertEquals(expected, TimeUtils.parseToLocalDate("2025-08-27"));
        assertEquals(expected, TimeUtils.parseToLocalDate("27-08-2025"));
        assertEquals(expected, TimeUtils.parseToLocalDate("2025/08/27"));
        assertEquals(expected, TimeUtils.parseToLocalDate("27/08/2025"));
    }
}

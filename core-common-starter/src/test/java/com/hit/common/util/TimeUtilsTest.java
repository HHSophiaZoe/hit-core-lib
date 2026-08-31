package com.hit.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    @Test
    void shouldNormalizeOffsetDateTimeToInstant() {
        assertEquals(
                Instant.parse("2026-08-31T03:00:00Z"),
                TimeUtils.parseToInstant("2026-08-31T10:00:00+07:00", DateTimeFormatter.ISO_DATE_TIME)
        );
    }
}

package com.hit.jpa.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlTransferUtilsTest {

    @Test
    void shouldNormalizeUtcAndOffsetValuesToTheSameInstant() {
        Instant utc = (Instant) SqlTransferUtils.castValueByClass(
                "2026-08-31T03:00:00Z", Instant.class);
        Instant offset = (Instant) SqlTransferUtils.castValueByClass(
                "2026-08-31T10:00:00+07:00", Instant.class);

        assertEquals(Instant.parse("2026-08-31T03:00:00Z"), utc);
        assertEquals(utc, offset);
    }

    @Test
    void shouldRejectInstantWithoutOffset() {
        assertThrows(IllegalArgumentException.class, () ->
                SqlTransferUtils.castValueByClass("2026-08-31T10:00:00", Instant.class));
    }

    @Test
    void shouldKeepBusinessLocalDateTimeWithoutTimezoneConversion() {
        LocalDateTime value = (LocalDateTime) SqlTransferUtils.castValueByClass(
                "2026-08-31T09:00:00", LocalDateTime.class);

        assertEquals(LocalDateTime.of(2026, 8, 31, 9, 0), value);
    }

    @Test
    void shouldRejectOffsetForBusinessLocalDateTime() {
        assertThrows(IllegalArgumentException.class, () ->
                SqlTransferUtils.castValueByClass(
                        "2026-08-31T09:00:00+07:00", LocalDateTime.class));
    }

    @Test
    void shouldRejectInvalidValueInsteadOfReturningNull() {
        assertThrows(IllegalArgumentException.class, () ->
                SqlTransferUtils.castValueByClass("not-a-date", Instant.class));
    }
}

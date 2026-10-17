package com.hit.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonUtilsTest {

    @Test
    void notEmptyOrBlankRejectsWhitespace() {
        assertThat(StringUtils.isNotEmptyOrBlank("a")).isTrue();
        assertThat(StringUtils.isNotEmptyOrBlank("  ")).isFalse();
        assertThat(StringUtils.isNotEmptyOrBlank("")).isFalse();
        assertThat(StringUtils.isNotEmptyOrBlank(null)).isFalse();
    }

    @Test
    void likeOperatorWrapsTheTrimmedValue() {
        assertThat(StringUtils.buildLikeOperator(" fpt ")).isEqualTo("%fpt%");
        assertThat(StringUtils.buildLikeOperatorUpper("fpt")).isEqualTo("%FPT%");
        assertThat(StringUtils.buildLikeOperator(" ")).isNull();
        assertThat(StringUtils.buildLikeOperator(null)).isNull();
    }

    @Test
    void removeAccentHandlesVietnamese() {
        assertThat(StringUtils.removeAccent("Đường Việt Nam")).isEqualTo("Duong Viet Nam");
    }

    @Test
    void roundUsesDecimalValueNotBinaryApproximation() {
        assertThat(NumberUtils.round(1.005)).isEqualTo(1.01);
        assertThat(NumberUtils.round(1910.255, 2)).isEqualTo(1910.26);
        assertThat(NumberUtils.safeRound(null)).isZero();
        assertThat(NumberUtils.round(null)).isNull();
    }

    @Test
    void sleepKeepsTheInterruptFlag() {
        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> ThreadUtils.sleep(Duration.ofSeconds(1))).isInstanceOf(IllegalStateException.class);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void toMapKeepsInsertionOrder() {
        Map<Integer, String> map = CollectionUtils.toMap(List.of("ccc", "a", "bb"), String::length);
        assertThat(map.keySet()).containsExactly(3, 1, 2);
    }

    @Test
    void invokeMethodByNameCallsTheMethodWithArguments() {
        assertThat(ReflectUtils.invokeMethodByName("hello", "length")).isEqualTo(5);
        assertThat(ReflectUtils.invokeMethodByName("hello", "charAt", 1)).isEqualTo('e');
    }

    @Test
    void legacyDateParseAndFormatUseTheSystemZone() {
        Date date = TimeUtils.parseToDate("17-10-2026 09:15:00", TimeUtils.VIETNAM_DATE_TIME_PATTERN);
        assertThat(date.toInstant()).isEqualTo(LocalDateTime.of(2026, 10, 17, 9, 15).atZone(ZoneId.systemDefault()).toInstant());
        assertThat(TimeUtils.formatDate(date, TimeUtils.DATE_TIME_PATTERN)).isEqualTo("2026-10-17 09:15:00");
        assertThat(TimeUtils.parseToDate("2026-10-17", TimeUtils.DATE_PATTERN).toInstant())
                .isEqualTo(LocalDateTime.of(2026, 10, 17, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        assertThat(TimeUtils.parseToDate("bad", TimeUtils.DATE_PATTERN)).isNull();
    }

    @Test
    void zipFolderContainsTheFiles(@TempDir Path dir) throws Exception {
        Path folder = Files.createDirectories(dir.resolve("reports"));
        Files.writeString(folder.resolve("a.txt"), "A");
        Files.writeString(folder.resolve("b.txt"), "B");

        byte[] zip = FileUtils.zipFolder(folder.toString());

        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            List<String> names = new java.util.ArrayList<>();
            for (var entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                names.add(entry.getName());
            }
            assertThat(names).containsExactlyInAnyOrder("reports/a.txt", "reports/b.txt");
        }
    }
}

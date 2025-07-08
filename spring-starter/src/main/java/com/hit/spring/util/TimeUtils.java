package com.hit.spring.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;

@Slf4j
@UtilityClass
public class TimeUtils {

    public static final String VIETNAM_DATE_TIME_PATTERN = "dd-MM-yyyy HH:mm:ss";
    public static final String VIETNAM_DATE_TIME_2_PATTERN = "dd-MM-yyyy HH:mm";
    public static final String ISO_DATE_TIME_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String ISO_DATE_TIME_UTC_2_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String ISO_OFFSET_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_DATE_PATTERN = "HH:mm:ss yyyy-MM-dd";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATE_TIME_ID_PATTERN = "yyyyMMddHHmmss";

    /*
     *
     * Methods parse date time
     *
     * */
    public static LocalDate parseToLocalDate(String dateStr, String pattern) {
        if (StringUtils.isEmptyOrBlank(dateStr)) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.error("parseToLocalDate ERROR", e);
            return null;
        }
    }

    public static LocalTime parseToLocalTime(String timeStr, String pattern) {
        if (StringUtils.isEmptyOrBlank(timeStr)) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalTime.parse(timeStr, formatter);
        } catch (Exception e) {
            log.error("parseToLocalTime ERROR", e);
            return null;
        }
    }

    public static LocalDateTime parseToLocalDateTime(String datetimeStr, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        if (StringUtils.isEmptyOrBlank(datetimeStr)) {
            return null;
        } else if (datetimeStr.contains(".")) {
            datetimeStr = datetimeStr.substring(0, datetimeStr.indexOf('.'));
        }
        try {
            return LocalDateTime.parse(datetimeStr, formatter);
        } catch (Exception e) {
            log.error("parseToLocalDateTime ERROR", e);
            return null;
        }
    }

    public static Date parseToDate(String dateStr, String pattern) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            return simpleDateFormat.parse(dateStr);
        } catch (Exception e) {
            log.error("parseToDate ERROR", e);
            return null;
        }
    }

    public static LocalDate parseToLocalDate(String input) {
        try {
            DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            String[] inputSplit1 = input.split("-");
            if (inputSplit1.length == 3) {
                if (inputSplit1[0].length() == 4) {
                    return LocalDate.parse(input, formatter1);
                } else {
                    return LocalDate.parse(input, formatter2);
                }
            } else if (input.split("-").length == 2) {
                return LocalDate.parse(input.concat("-" + now().getYear()), formatter2);
            }

            String[] inputSplit2 = input.split("/");
            if (inputSplit2.length == 3) {
                if (inputSplit1[0].length() == 4) {
                    return LocalDate.parse(input, formatter3);
                } else {
                    return LocalDate.parse(input, formatter4);
                }
            } else if (inputSplit2.length == 2) {
                return LocalDate.parse(input.concat("-" + now().getYear()), formatter4);
            }
            return LocalDate.parse(input.concat("-" + now().getMonthValue() + "-" + now().getYear()), formatter2);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDateTime parseStringToLocalDateTime(String input) {
        return Stream.of(
                        ofPattern(VIETNAM_DATE_TIME_PATTERN),
                        ofPattern(VIETNAM_DATE_TIME_2_PATTERN),
                        ofPattern(ISO_DATE_TIME_UTC_PATTERN),
                        ofPattern(ISO_DATE_TIME_UTC_2_PATTERN),
                        ofPattern(ISO_OFFSET_DATE_TIME_PATTERN),
                        ofPattern(DATE_TIME_PATTERN),
                        ofPattern(TIME_DATE_PATTERN))
                .map(dateTimeFormatter -> {
                    try {
                        return LocalDateTime.parse(input, dateTimeFormatter);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /*
     *
     * Methods format date time
     *
     * */
    public static String formatLocalDate(LocalDate date, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return formatter.format(date);
        } catch (Exception e) {
            log.error("formatLocalDate ERROR", e);
            return null;
        }
    }

    public static String formatLocalDateTime(LocalDateTime datetime, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return formatter.format(datetime);
        } catch (Exception e) {
            log.error("formatLocalDateTime ERROR", e);
            return null;
        }
    }

    public static String formatDate(Date date, String pattern) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            return simpleDateFormat.format(date);
        } catch (Exception e) {
            log.error("formatDate ERROR", e);
            return null;
        }
    }

    /*
     *
     * Methods convert date time
     *
     * */
    public static LocalDate convertDateToLocalDate(Date date) {
        try {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            log.error("convertDateToLocalDate ERROR", e);
            return null;
        }
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        try {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.error("convertDateToLocalDateTime ERROR", e);
            return null;
        }
    }


    /*
     *
     * Methods calculate date
     *
     * */
    public static Integer getDaysBetween(LocalDateTime start, LocalDateTime end) {
        return Math.toIntExact(Math.round(ChronoUnit.HOURS.between(start, end) / 24d));
    }
}

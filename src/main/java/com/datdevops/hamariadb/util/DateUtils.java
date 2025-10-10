package com.datdevops.hamariadb.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private DateUtils() {
        // Private constructor to prevent instantiation
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT);

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    public static LocalDate parseDate(String dateStr) {
        return dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : null;
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return dateTimeStr != null ? LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER) : null;
    }

    public static LocalDateTime getStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    public static LocalDateTime getEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59, 999999999) : null;
    }

    public static boolean isWithinRange(LocalDateTime date, LocalDateTime start, LocalDateTime end) {
        if (date == null) return false;
        if (start != null && date.isBefore(start)) return false;
        if (end != null && date.isAfter(end)) return false;
        return true;
    }
}

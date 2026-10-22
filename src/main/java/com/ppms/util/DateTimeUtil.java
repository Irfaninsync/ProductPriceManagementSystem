// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Standardizes date and time serialization, parsing, and display formats.
 */
public class DateTimeUtil {

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatIso(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(ISO_FORMATTER);
    }

    public static LocalDateTime parseIso(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(isoString.trim(), ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            // Fallback for standard ISO-8601 with fractional seconds if present
            return LocalDateTime.parse(isoString.trim());
        }
    }

    public static String formatDisplay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DISPLAY_FORMATTER);
    }

    public static String formatDateOnly(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DATE_ONLY_FORMATTER);
    }

    public static LocalDate parseDateOnly(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_ONLY_FORMATTER);
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem


package ua.edu.nupp.servetra.server.service;

import ua.edu.nupp.servetra.server.error.ApiException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

// [ETAP 2] Shared server validation
// Requirement: Enforce required fields, ranges, normalization, and non-negative values.
public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private Validation() {
    }

    public static String required(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw ApiException.validation(field + " is required");
        }
        if (normalized.length() > maxLength) {
            throw ApiException.validation(field + " is too long");
        }
        return normalized;
    }

    public static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, field, maxLength);
    }

    public static String email(String value) {
        String normalized = required(value, "email", 190).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw ApiException.validation("email is invalid");
        }
        return normalized;
    }

    public static int score(int value, String field) {
        if (value < 0 || value > 100) {
            throw ApiException.validation(field + " must be in range 0..100");
        }
        return value;
    }

    public static long positive(long value, String field) {
        if (value <= 0) {
            throw ApiException.validation(field + " must be positive");
        }
        return value;
    }

    public static Integer nonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw ApiException.validation(field + " must be non-negative");
        }
        return value;
    }

    public static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw ApiException.validation(field + " must be non-negative");
        }
        return value;
    }
}


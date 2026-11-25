package ua.edu.nupp.servetra.client.validation;

import java.math.BigDecimal;

// [ETAP 3] Client-side input validation
// Requirement: Reject malformed form values before an HTTP request is sent.
public final class FormParsers {
    private FormParsers() {
    }

    public static long positiveLong(String value, String field) {
        try {
            long result = Long.parseLong(value.trim());
            if (result <= 0) {
                throw new NumberFormatException();
            }
            return result;
        } catch (Exception error) {
            throw new IllegalArgumentException(field + " must be a positive number");
        }
    }

    public static Long optionalPositiveLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return positiveLong(value, "id");
    }

    public static Integer optionalNonNegativeInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result < 0) {
                throw new NumberFormatException();
            }
            return result;
        } catch (Exception error) {
            throw new IllegalArgumentException("duration must be a non-negative number");
        }
    }

    public static BigDecimal optionalNonNegativeDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal result = new BigDecimal(value.trim());
            if (result.signum() < 0) {
                throw new NumberFormatException();
            }
            return result;
        } catch (Exception error) {
            throw new IllegalArgumentException("cost must be a non-negative number");
        }
    }
}

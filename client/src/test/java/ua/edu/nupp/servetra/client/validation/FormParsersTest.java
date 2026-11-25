package ua.edu.nupp.servetra.client.validation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// [ETAP 4] JUnit validation coverage
// Requirement: Exercise real client-side parsing without JavaFX or external services.
class FormParsersTest {
    @Test void positiveIdIsParsed() {
        assertEquals(42L, FormParsers.positiveLong(" 42 ", "id"));
    }

    @Test void zeroIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FormParsers.positiveLong("0", "id"));
    }

    @Test void nonNumericIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FormParsers.positiveLong("abc", "id"));
    }

    @Test void blankOptionalIdBecomesNull() {
        assertNull(FormParsers.optionalPositiveLong(" "));
    }

    @Test void durationMustBeNonNegative() {
        assertAll(
                () -> assertEquals(0, FormParsers.optionalNonNegativeInt("0")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> FormParsers.optionalNonNegativeInt("-1")));
    }

    @Test void costAcceptsDecimalValue() {
        assertEquals(new BigDecimal("19.95"), FormParsers.optionalNonNegativeDecimal("19.95"));
    }

    @Test void negativeCostIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FormParsers.optionalNonNegativeDecimal("-0.01"));
    }
}

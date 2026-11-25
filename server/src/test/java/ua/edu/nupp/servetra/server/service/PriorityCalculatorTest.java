package ua.edu.nupp.servetra.server.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PriorityCalculatorTest {
    @Test void zeroFactorsProduceZero() { assertEquals(new BigDecimal("0.00"), PriorityCalculator.calculate(0, 0, 0)); }
    @Test void maximumFactorsProduceOneHundred() { assertEquals(new BigDecimal("100.00"), PriorityCalculator.calculate(100, 100, 100)); }
    @Test void documentedExampleProducesSeventyNine() { assertEquals(new BigDecimal("79.00"), PriorityCalculator.calculate(90, 80, 60)); }
    @Test void weightsAreAppliedInCorrectOrder() { assertEquals(new BigDecimal("40.00"), PriorityCalculator.calculate(100, 0, 0)); }
    @Test void negativeFactorIsRejected() { assertThrows(RuntimeException.class, () -> PriorityCalculator.calculate(-1, 0, 0)); }
    @Test void factorAboveOneHundredIsRejected() { assertThrows(RuntimeException.class, () -> PriorityCalculator.calculate(0, 101, 0)); }
    @Test void decimalResultUsesTwoPlaces() { assertEquals(new BigDecimal("33.25"), PriorityCalculator.calculate(33, 33, 34)); }
    @Test void categoriesRespectBoundaries() {
        assertAll(
                () -> assertEquals("LOW", PriorityCalculator.category(new BigDecimal("24.99"))),
                () -> assertEquals("MEDIUM", PriorityCalculator.category(new BigDecimal("25.00"))),
                () -> assertEquals("HIGH", PriorityCalculator.category(new BigDecimal("50.00"))),
                () -> assertEquals("CRITICAL", PriorityCalculator.category(new BigDecimal("75.00"))));
    }
}


package ua.edu.nupp.servetra.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// [ETAP 2] Business logic
// Requirement: Calculate I = 0.40F1 + 0.35F2 + 0.25F3 for normalized factors.
public final class PriorityCalculator {
    private static final BigDecimal W1 = new BigDecimal("0.40");
    private static final BigDecimal W2 = new BigDecimal("0.35");
    private static final BigDecimal W3 = new BigDecimal("0.25");
    private static final BigDecimal MEDIUM = new BigDecimal("25");
    private static final BigDecimal HIGH = new BigDecimal("50");
    private static final BigDecimal CRITICAL = new BigDecimal("75");

    private PriorityCalculator() {
    }

    public static BigDecimal calculate(int f1, int f2, int f3) {
        Validation.score(f1, "F1");
        Validation.score(f2, "F2");
        Validation.score(f3, "F3");
        BigDecimal firstPart = BigDecimal.valueOf(f1).multiply(W1);
        BigDecimal secondPart = BigDecimal.valueOf(f2).multiply(W2);
        BigDecimal thirdPart = BigDecimal.valueOf(f3).multiply(W3);
        BigDecimal result = firstPart.add(secondPart).add(thirdPart);
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    public static String category(BigDecimal index) {
        if (index == null) {
            throw new IllegalArgumentException("index is required");
        }
        if (index.compareTo(MEDIUM) < 0) {
            return "LOW";
        }
        if (index.compareTo(HIGH) < 0) {
            return "MEDIUM";
        }
        if (index.compareTo(CRITICAL) < 0) {
            return "HIGH";
        }
        return "CRITICAL";
    }
}


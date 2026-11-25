import ua.edu.nupp.servetra.server.model.RequestStatus;
import ua.edu.nupp.servetra.server.service.Lifecycle;
import ua.edu.nupp.servetra.server.service.PriorityCalculator;

import java.math.BigDecimal;

public class CoreSelfTest {
    private static int checks;
    public static void main(String[] args) {
        eq(new BigDecimal("0.00"), PriorityCalculator.calculate(0, 0, 0));
        eq(new BigDecimal("100.00"), PriorityCalculator.calculate(100, 100, 100));
        eq(new BigDecimal("79.00"), PriorityCalculator.calculate(90, 80, 60));
        truth(Lifecycle.isAllowed(RequestStatus.NEW, RequestStatus.DIAGNOSED));
        truth(Lifecycle.isAllowed(RequestStatus.DIAGNOSED, RequestStatus.IN_SERVICE));
        truth(Lifecycle.isAllowed(RequestStatus.IN_SERVICE, RequestStatus.COMPLETED));
        truth(!Lifecycle.isAllowed(RequestStatus.NEW, RequestStatus.COMPLETED));
        truth(!Lifecycle.isAllowed(RequestStatus.COMPLETED, RequestStatus.NEW));
        System.out.println("Core self-test passed: " + checks + " checks");
    }
    private static void eq(Object expected, Object actual) { checks++; if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual); }
    private static void truth(boolean value) { checks++; if (!value) throw new AssertionError("condition failed"); }
}


import ua.edu.nupp.servetra.client.importer.EquipmentCsvMapper;
import ua.edu.nupp.servetra.client.importer.SemicolonCsv;

import java.time.LocalDate;

public class CsvSelfTest {
    private static int checks;

    public static void main(String[] args) {
        var equipment = EquipmentCsvMapper.parse(
                "EQ-42;Microscope;Optics;Acme;M1;SN-42;Lab A;Physics;75;2027-03-01");
        eq("EQ-42", equipment.inventoryNumber());
        eq(75, equipment.criticalityScore());
        eq(LocalDate.of(2027, 3, 1), equipment.nextServiceDue());
        eq("Laboratory; East", SemicolonCsv.parseLine("EQ-1;\"Laboratory; East\"").get(1));
        eq("A \"quoted\" value", SemicolonCsv.parseLine("\"A \"\"quoted\"\" value\"").get(0));
        throwsIllegalArgument(() -> EquipmentCsvMapper.parse(
                "EQ-1;Device;Category;;;;Lab;;101;2027-01-01"));
        System.out.println("CSV self-test passed: " + checks + " checks");
    }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }

    private static void throwsIllegalArgument(Runnable action) {
        checks++;
        try {
            action.run();
            throw new AssertionError("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}

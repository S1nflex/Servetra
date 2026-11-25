package ua.edu.nupp.servetra.client.importer;

import ua.edu.nupp.servetra.client.model.Models.Equipment;
import java.time.LocalDate;
import java.util.List;

// [ETAP 4] CSV row validation and mapping for the equipment registry.
public final class EquipmentCsvMapper {
    public static final String HEADER = "inventory_number;name;category;manufacturer;model;serial_number;location;department;criticality_score;next_service_due";

    private EquipmentCsvMapper() {
    }

    public static Equipment parse(String line) {
        List<String> fields = SemicolonCsv.parseLine(line);
        if (fields.size() != 10) {
            throw new IllegalArgumentException("Expected 10 columns");
        }

        String inventoryNumber = fields.get(0);
        String name = fields.get(1);
        String category = fields.get(2);
        String location = fields.get(6);
        if (inventoryNumber.isBlank() || name.isBlank()
                || category.isBlank() || location.isBlank()) {
            throw new IllegalArgumentException("Required value is missing");
        }

        int criticality = Integer.parseInt(fields.get(8));
        if (criticality < 0 || criticality > 100) {
            throw new IllegalArgumentException("criticality_score must be 0..100");
        }

        LocalDate due = null;
        if (!fields.get(9).isBlank()) {
            due = LocalDate.parse(fields.get(9));
        }

        return new Equipment(
                null,
                inventoryNumber,
                name,
                category,
                blank(fields.get(3)),
                blank(fields.get(4)),
                blank(fields.get(5)),
                location,
                blank(fields.get(7)),
                criticality,
                due,
                true
        );
    }

    private static String blank(String value) {
        if (value.isBlank()) {
            return null;
        }
        return value;
    }
}


package ua.edu.nupp.servetra.server.service;

import org.junit.jupiter.api.Test;
import ua.edu.nupp.servetra.server.model.Equipment;
import ua.edu.nupp.servetra.server.model.Specialist;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test void equipmentInventoryNumberIsRequired() {
        Equipment value = new Equipment(null, " ", "Microscope", "LAB", null, null, null, "Lab", null, 50, null, true);
        assertThrows(RuntimeException.class, () -> EquipmentService.validated(null, value));
    }
    @Test void equipmentCriticalityIsValidated() {
        Equipment value = new Equipment(null, "EQ-1", "Microscope", "LAB", null, null, null, "Lab", null, 101, null, true);
        assertThrows(RuntimeException.class, () -> EquipmentService.validated(null, value));
    }
    @Test void equipmentIsNormalized() {
        Equipment value = new Equipment(null, " eq-1 ", "  Digital   microscope ", "lab", "", "", "", " Lab 1 ", "", 70, LocalDate.of(2027, 1, 1), true);
        Equipment normalized = EquipmentService.validated(4L, value);
        assertAll(() -> assertEquals("EQ-1", normalized.inventoryNumber()), () -> assertEquals("Digital microscope", normalized.name()), () -> assertNull(normalized.manufacturer()));
    }
    @Test void specialistEmailIsRequired() {
        Specialist value = new Specialist(null, "Olena Koval", "", null, null, true);
        assertThrows(RuntimeException.class, () -> SpecialistService.validated(null, value));
    }
    @Test void specialistEmailMustBeValid() {
        Specialist value = new Specialist(null, "Olena Koval", "not-an-email", null, null, true);
        assertThrows(RuntimeException.class, () -> SpecialistService.validated(null, value));
    }
    @Test void specialistEmailIsNormalized() {
        Specialist value = new Specialist(null, "Olena Koval", " OLENA@EXAMPLE.EDU ", null, null, true);
        assertEquals("olena@example.edu", SpecialistService.validated(2L, value).email());
    }
}


package ua.edu.nupp.servetra.client.importer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentCsvMapperTest {
    @Test void validRowMapsEquipment() {
        var value = EquipmentCsvMapper.parse("LAB-1;Microscope;LAB;Leica;DM750;S-1;Lab 12;Biology;75;2027-01-20");
        assertAll(() -> assertEquals("LAB-1", value.inventoryNumber()), () -> assertEquals(75, value.criticalityScore()), () -> assertEquals(2027, value.nextServiceDue().getYear()));
    }
    @Test void emptyOptionalValuesAreNull() { assertNull(EquipmentCsvMapper.parse("LAB-1;Microscope;LAB;;;;Lab 12;;75;").manufacturer()); }
    @Test void criticalityAboveRangeRejected() { assertThrows(IllegalArgumentException.class, () -> EquipmentCsvMapper.parse("LAB-1;Microscope;LAB;;;;Lab 12;;101;")); }
    @Test void missingNameRejected() { assertThrows(IllegalArgumentException.class, () -> EquipmentCsvMapper.parse("LAB-1;;LAB;;;;Lab 12;;75;")); }
    @Test void quotedSemicolonIsPreserved() { assertEquals("Lab; west", SemicolonCsv.parseLine("A;\"Lab; west\";C").get(1)); }
    @Test void doubledQuoteIsDecoded() { assertEquals("A \"quoted\" value", SemicolonCsv.parseLine("\"A \"\"quoted\"\" value\"").get(0)); }
    @Test void wrongColumnCountIsRejected() { assertThrows(IllegalArgumentException.class, () -> EquipmentCsvMapper.parse("LAB-1;Only two columns")); }
    @Test void invalidNumberIsRejected() { assertThrows(NumberFormatException.class, () -> EquipmentCsvMapper.parse("LAB-1;Microscope;LAB;;;;Lab 12;;high;")); }
    @Test void invalidDateIsRejected() { assertThrows(RuntimeException.class, () -> EquipmentCsvMapper.parse("LAB-1;Microscope;LAB;;;;Lab 12;;50;20-01-2027")); }
    @Test void unclosedQuoteIsRejected() { assertThrows(IllegalArgumentException.class, () -> SemicolonCsv.parseLine("A;\"not closed")); }
}


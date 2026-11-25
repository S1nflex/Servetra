package ua.edu.nupp.servetra.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ua.edu.nupp.servetra.server.dao.*;
import ua.edu.nupp.servetra.server.model.*;
import ua.edu.nupp.servetra.server.service.PriorityCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// [ETAP 4] Optional MySQL integration verification
// Requirement: Prove that all four DAO classes perform real CRUD against the recreated schema.
@Tag("integration")
class DaoCrudIT {
    @Test void allDaoCrudOperationsRoundTripThroughMySql() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv("SERVETRA_RUN_INTEGRATION")),
                "Set SERVETRA_RUN_INTEGRATION=true to execute MySQL integration tests");

        EquipmentDao equipmentDao = new EquipmentDao();
        SpecialistDao specialistDao = new SpecialistDao();
        MaintenanceRequestDao requestDao = new MaintenanceRequestDao();
        ServiceRecordDao recordDao = new ServiceRecordDao();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Equipment equipment = null;
        Specialist specialist = null;
        MaintenanceRequest request = null;
        ServiceRecord record = null;
        try {
            equipment = equipmentDao.create(new Equipment(null, "IT-" + suffix, "Integration device",
                    "LAB", "SERVETRA", "IT", null, "Integration lab", "QA", 80,
                    LocalDate.now().plusMonths(1), true));
            specialist = specialistDao.create(new Specialist(null, "Integration Specialist",
                    "integration-" + suffix + "@example.edu", "QA", null, true));
            request = requestDao.create(new MaintenanceRequest(null, null, equipment.id(), specialist.id(),
                    "Integration request", "Created by DaoCrudIT", null, RequestStatus.NEW,
                    80, 70, 60, PriorityCalculator.calculate(80, 70, 60),
                    LocalDateTime.now().plusDays(1), null, null, null));
            record = recordDao.create(new ServiceRecord(null, request.id(), specialist.id(),
                    WorkType.DIAGNOSTICS, "Integration diagnostics", LocalDateTime.now(),
                    15, new BigDecimal("0.00")));

            Equipment createdEquipment = equipment;
            Specialist createdSpecialist = specialist;
            MaintenanceRequest createdRequest = request;
            ServiceRecord createdRecord = record;

            assertAll(
                    () -> assertEquals("Integration device", equipmentDao.findById(createdEquipment.id()).orElseThrow().name()),
                    () -> assertEquals("Integration Specialist", specialistDao.findById(createdSpecialist.id()).orElseThrow().fullName()),
                    () -> assertEquals(RequestStatus.NEW, requestDao.findById(createdRequest.id()).orElseThrow().status()),
                    () -> assertEquals(WorkType.DIAGNOSTICS, recordDao.findById(createdRecord.id()).orElseThrow().workType()));

            Equipment updated = equipmentDao.update(new Equipment(equipment.id(), equipment.inventoryNumber(),
                    "Updated integration device", equipment.category(), equipment.manufacturer(), equipment.model(),
                    equipment.serialNumber(), equipment.location(), equipment.department(), 85,
                    equipment.nextServiceDue(), true));
            MaintenanceRequest diagnosed = requestDao.transition(request.id(), RequestStatus.DIAGNOSED);
            assertEquals("Updated integration device", updated.name());
            assertEquals(RequestStatus.DIAGNOSED, diagnosed.status());
        } finally {
            if (record != null) recordDao.delete(record.id());
            if (request != null) requestDao.delete(request.id());
            if (specialist != null) specialistDao.delete(specialist.id());
            if (equipment != null) equipmentDao.delete(equipment.id());
        }
    }
}

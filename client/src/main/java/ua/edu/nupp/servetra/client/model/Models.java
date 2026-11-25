package ua.edu.nupp.servetra.client.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// [ETAP 3] Client JSON models matching server response contracts.
public final class Models {
    private Models() {}

    public record Equipment(Long id, String inventoryNumber, String name, String category,
                            String manufacturer, String model, String serialNumber, String location,
                            String department, int criticalityScore, LocalDate nextServiceDue, boolean active) {}

    public record Specialist(Long id, String fullName, String email, String specialization, String phone, boolean active) {}

    public enum RequestStatus { NEW, DIAGNOSED, IN_SERVICE, COMPLETED }

    public record MaintenanceRequest(Long id, String requestNumber, long equipmentId, Long specialistId,
                                     String summary, String description, String diagnosticNotes, RequestStatus status,
                                     int criticalitySnapshot, int severityScore, int downtimeImpactScore,
                                     BigDecimal priorityIndex, LocalDateTime dueAt, LocalDateTime createdAt,
                                     LocalDateTime updatedAt, LocalDateTime completedAt) {}

    public enum WorkType { DIAGNOSTICS, REPAIR, PREVENTIVE, INSPECTION }

    public record ServiceRecord(Long id, long requestId, long specialistId, WorkType workType,
                                String description, LocalDateTime performedAt, Integer durationMinutes,
                                BigDecimal costAmount) {}
}


package ua.edu.nupp.servetra.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// [ETAP 1] Data model
// Requirement: Represent a maintenance request with lifecycle state and normalized factors.
public record MaintenanceRequest(
        Long id,
        String requestNumber,
        long equipmentId,
        Long specialistId,
        String summary,
        String description,
        String diagnosticNotes,
        RequestStatus status,
        int criticalitySnapshot,
        int severityScore,
        int downtimeImpactScore,
        BigDecimal priorityIndex,
        LocalDateTime dueAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {}


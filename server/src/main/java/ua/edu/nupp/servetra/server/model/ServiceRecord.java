package ua.edu.nupp.servetra.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// [ETAP 1] Data model
// Requirement: Store factual maintenance work linked to a request and specialist.
public record ServiceRecord(
        Long id,
        long requestId,
        long specialistId,
        WorkType workType,
        String description,
        LocalDateTime performedAt,
        Integer durationMinutes,
        BigDecimal costAmount
) {}


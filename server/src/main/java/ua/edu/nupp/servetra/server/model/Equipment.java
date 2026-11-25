package ua.edu.nupp.servetra.server.model;

import java.time.LocalDate;

// [ETAP 1] Data model
// Requirement: Represent registered laboratory equipment and its F1 criticality.
public record Equipment(
        Long id,
        String inventoryNumber,
        String name,
        String category,
        String manufacturer,
        String model,
        String serialNumber,
        String location,
        String department,
        int criticalityScore,
        LocalDate nextServiceDue,
        boolean active
) {}


package ua.edu.nupp.servetra.server.model;

// [ETAP 1] Data model
// Requirement: Represent a specialist assigned to requests and service records.
public record Specialist(
        Long id,
        String fullName,
        String email,
        String specialization,
        String phone,
        boolean active
) {}


package ua.edu.nupp.servetra.server.model;

// [ETAP 1] Four-state lifecycle vocabulary for MaintenanceRequest.
public enum RequestStatus {
    NEW,
    DIAGNOSED,
    IN_SERVICE,
    COMPLETED
}


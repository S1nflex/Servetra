package ua.edu.nupp.servetra.server.service;

import ua.edu.nupp.servetra.server.model.RequestStatus;

// [ETAP 2] Business logic
// Requirement: Allow only NEW -> DIAGNOSED -> IN_SERVICE -> COMPLETED.
public final class Lifecycle {
    private Lifecycle() {
    }

    public static boolean isAllowed(RequestStatus from, RequestStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == RequestStatus.NEW) {
            return to == RequestStatus.DIAGNOSED;
        }
        if (from == RequestStatus.DIAGNOSED) {
            return to == RequestStatus.IN_SERVICE;
        }
        if (from == RequestStatus.IN_SERVICE) {
            return to == RequestStatus.COMPLETED;
        }
        return false;
    }
}


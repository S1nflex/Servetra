package ua.edu.nupp.servetra.server.service;

import org.junit.jupiter.api.Test;
import ua.edu.nupp.servetra.server.model.RequestStatus;
import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {
    @Test void newToDiagnosedAllowed() { assertTrue(Lifecycle.isAllowed(RequestStatus.NEW, RequestStatus.DIAGNOSED)); }
    @Test void diagnosedToInServiceAllowed() { assertTrue(Lifecycle.isAllowed(RequestStatus.DIAGNOSED, RequestStatus.IN_SERVICE)); }
    @Test void inServiceToCompletedAllowed() { assertTrue(Lifecycle.isAllowed(RequestStatus.IN_SERVICE, RequestStatus.COMPLETED)); }
    @Test void skippingStatesRejected() { assertFalse(Lifecycle.isAllowed(RequestStatus.NEW, RequestStatus.COMPLETED)); }
    @Test void backwardTransitionRejected() { assertFalse(Lifecycle.isAllowed(RequestStatus.IN_SERVICE, RequestStatus.NEW)); }
    @Test void repeatedStateRejected() { assertFalse(Lifecycle.isAllowed(RequestStatus.DIAGNOSED, RequestStatus.DIAGNOSED)); }
    @Test void completedIsFinal() { assertFalse(Lifecycle.isAllowed(RequestStatus.COMPLETED, RequestStatus.NEW)); }
    @Test void nullTransitionRejected() { assertFalse(Lifecycle.isAllowed(null, RequestStatus.NEW)); }
}


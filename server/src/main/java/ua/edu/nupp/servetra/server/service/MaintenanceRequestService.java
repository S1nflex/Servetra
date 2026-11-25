package ua.edu.nupp.servetra.server.service;

import ua.edu.nupp.servetra.server.dao.EquipmentDao;
import ua.edu.nupp.servetra.server.dao.MaintenanceRequestDao;
import ua.edu.nupp.servetra.server.dao.SpecialistDao;
import ua.edu.nupp.servetra.server.error.ApiException;
import ua.edu.nupp.servetra.server.model.Equipment;
import ua.edu.nupp.servetra.server.model.MaintenanceRequest;
import ua.edu.nupp.servetra.server.model.RequestStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

// [ETAP 2] Maintenance request business service
// Requirement: Resolve references, snapshot F1, calculate I, and enforce lifecycle transitions.
public class MaintenanceRequestService {
    private static final Logger LOG = Logger.getLogger(MaintenanceRequestService.class.getName());
    private final MaintenanceRequestDao dao = new MaintenanceRequestDao();
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final SpecialistDao specialistDao = new SpecialistDao();

    public List<MaintenanceRequest> list(RequestStatus status) throws SQLException {
        return dao.findAll(status);
    }

    public MaintenanceRequest get(long id) throws SQLException {
        return dao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Maintenance request was not found"));
    }

    public MaintenanceRequest create(MaintenanceRequest input) throws SQLException {
        Equipment equipment = requireReferences(input);
        MaintenanceRequest value = normalized(
                null,
                null,
                input,
                RequestStatus.NEW,
                equipment.criticalityScore()
        );
        MaintenanceRequest created = dao.create(value);
        LOG.info("Created maintenance request id=" + created.id());
        return created;
    }

    public MaintenanceRequest update(long id, MaintenanceRequest input) throws SQLException {
        MaintenanceRequest old = get(id);
        Equipment equipment = requireReferences(input);

        int snapshot;
        if (old.equipmentId() == input.equipmentId()) {
            snapshot = old.criticalitySnapshot();
        } else {
            snapshot = equipment.criticalityScore();
        }

        MaintenanceRequest value = normalized(
                id,
                old.requestNumber(),
                input,
                old.status(),
                snapshot
        );
        MaintenanceRequest updated = dao.update(value);
        LOG.info("Updated maintenance request id=" + id);
        return updated;
    }

    public MaintenanceRequest transition(long id, RequestStatus next) throws SQLException {
        MaintenanceRequest current = get(id);
        if (!Lifecycle.isAllowed(current.status(), next)) {
            String message = "Transition " + current.status() + " -> " + next + " is not allowed";
            throw ApiException.conflict("INVALID_TRANSITION", message);
        }
        MaintenanceRequest result = dao.transition(id, next);
        LOG.info("Transitioned request id=" + id + " to " + next);
        return result;
    }

    public void delete(long id) throws SQLException {
        boolean deleted = dao.delete(id);
        if (!deleted) {
            throw ApiException.notFound("Maintenance request was not found");
        }
        LOG.info("Deleted maintenance request id=" + id);
    }

    private Equipment requireReferences(MaintenanceRequest input) throws SQLException {
        if (input == null) {
            throw ApiException.validation("maintenance request body is required");
        }
        Validation.positive(input.equipmentId(), "equipmentId");
        Equipment equipment = equipmentDao.findById(input.equipmentId())
                .orElseThrow(() -> ApiException.validation("equipmentId does not exist"));
        if (input.specialistId() != null && specialistDao.findById(input.specialistId()).isEmpty()) {
            throw ApiException.validation("specialistId does not exist");
        }
        return equipment;
    }

    private static MaintenanceRequest normalized(
            Long id,
            String number,
            MaintenanceRequest v,
            RequestStatus status,
            int f1
    ) {
        int f2 = Validation.score(v.severityScore(), "severityScore");
        int f3 = Validation.score(v.downtimeImpactScore(), "downtimeImpactScore");
        BigDecimal priority = PriorityCalculator.calculate(f1, f2, f3);
        LocalDateTime completedAt = null;
        if (status == RequestStatus.COMPLETED) {
            completedAt = v.completedAt();
        }
        return new MaintenanceRequest(
                id,
                number,
                v.equipmentId(),
                v.specialistId(),
                Validation.required(v.summary(), "summary", 180),
                Validation.optional(v.description(), "description", 4000),
                Validation.optional(v.diagnosticNotes(), "diagnosticNotes", 4000),
                status,
                f1,
                f2,
                f3,
                priority,
                v.dueAt(),
                null,
                null,
                completedAt
        );
    }
}


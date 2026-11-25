package ua.edu.nupp.servetra.server.service;

import ua.edu.nupp.servetra.server.dao.MaintenanceRequestDao;
import ua.edu.nupp.servetra.server.dao.ServiceRecordDao;
import ua.edu.nupp.servetra.server.dao.SpecialistDao;
import ua.edu.nupp.servetra.server.error.ApiException;
import ua.edu.nupp.servetra.server.model.ServiceRecord;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

// [ETAP 2] Service record business service
// Requirement: Validate references, work details, duration, and cost.
public class ServiceRecordService {
    private static final Logger LOG = Logger.getLogger(ServiceRecordService.class.getName());
    private final ServiceRecordDao dao = new ServiceRecordDao();
    private final MaintenanceRequestDao requestDao = new MaintenanceRequestDao();
    private final SpecialistDao specialistDao = new SpecialistDao();

    public List<ServiceRecord> list(Long requestId) throws SQLException {
        return dao.findAll(requestId);
    }

    public ServiceRecord get(long id) throws SQLException {
        return dao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Service record was not found"));
    }

    public ServiceRecord create(ServiceRecord input) throws SQLException {
        ServiceRecord value = validated(null, input);
        ServiceRecord created = dao.create(value);
        LOG.info("Created service record id=" + created.id());
        return created;
    }

    public ServiceRecord update(long id, ServiceRecord input) throws SQLException {
        get(id);
        ServiceRecord value = validated(id, input);
        ServiceRecord updated = dao.update(value);
        LOG.info("Updated service record id=" + id);
        return updated;
    }

    public void delete(long id) throws SQLException {
        boolean deleted = dao.delete(id);
        if (!deleted) {
            throw ApiException.notFound("Service record was not found");
        }
        LOG.info("Deleted service record id=" + id);
    }

    private ServiceRecord validated(Long id, ServiceRecord v) throws SQLException {
        if (v == null) {
            throw ApiException.validation("service record body is required");
        }
        Validation.positive(v.requestId(), "requestId");
        Validation.positive(v.specialistId(), "specialistId");
        if (requestDao.findById(v.requestId()).isEmpty()) {
            throw ApiException.validation("requestId does not exist");
        }
        if (specialistDao.findById(v.specialistId()).isEmpty()) {
            throw ApiException.validation("specialistId does not exist");
        }
        if (v.workType() == null) {
            throw ApiException.validation("workType is required");
        }
        LocalDateTime performed = v.performedAt() == null ? LocalDateTime.now() : v.performedAt();
        return new ServiceRecord(
                id,
                v.requestId(),
                v.specialistId(),
                v.workType(),
                Validation.required(v.description(), "description", 4000),
                performed,
                Validation.nonNegative(v.durationMinutes(), "durationMinutes"),
                Validation.nonNegative(v.costAmount(), "costAmount")
        );
    }
}


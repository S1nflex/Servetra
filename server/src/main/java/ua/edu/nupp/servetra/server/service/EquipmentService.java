package ua.edu.nupp.servetra.server.service;

import ua.edu.nupp.servetra.server.dao.EquipmentDao;
import ua.edu.nupp.servetra.server.error.ApiException;
import ua.edu.nupp.servetra.server.model.Equipment;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

// [ETAP 2] Equipment business service
// Requirement: Validate and normalize equipment before DAO operations.
public class EquipmentService {
    private static final Logger LOG = Logger.getLogger(EquipmentService.class.getName());
    private final EquipmentDao dao;

    public EquipmentService() {
        this(new EquipmentDao());
    }

    EquipmentService(EquipmentDao dao) {
        this.dao = dao;
    }

    public List<Equipment> list(String query) throws SQLException {
        return dao.findAll(query);
    }

    public Equipment get(long id) throws SQLException {
        return dao.findById(id).orElseThrow(() -> ApiException.notFound("Equipment was not found"));
    }

    public Equipment create(Equipment input) throws SQLException {
        Equipment value = validated(null, input);
        Equipment created = dao.create(value);
        LOG.info("Created equipment id=" + created.id());
        return created;
    }

    public Equipment update(long id, Equipment input) throws SQLException {
        get(id);
        Equipment value = validated(id, input);
        Equipment updated = dao.update(value);
        LOG.info("Updated equipment id=" + id);
        return updated;
    }

    public void delete(long id) throws SQLException {
        boolean deleted = dao.delete(id);
        if (!deleted) {
            throw ApiException.notFound("Equipment was not found");
        }
        LOG.info("Deleted equipment id=" + id);
    }

    public static Equipment validated(Long id, Equipment v) {
        if (v == null) {
            throw ApiException.validation("equipment body is required");
        }
        return new Equipment(id,
                Validation.required(v.inventoryNumber(), "inventoryNumber", 64).toUpperCase(Locale.ROOT),
                Validation.required(v.name(), "name", 160),
                Validation.required(v.category(), "category", 80).toUpperCase(Locale.ROOT),
                Validation.optional(v.manufacturer(), "manufacturer", 100),
                Validation.optional(v.model(), "model", 100),
                Validation.optional(v.serialNumber(), "serialNumber", 120),
                Validation.required(v.location(), "location", 160),
                Validation.optional(v.department(), "department", 120),
                Validation.score(v.criticalityScore(), "criticalityScore"),
                v.nextServiceDue(), v.active());
    }
}


package ua.edu.nupp.servetra.server.service;

import ua.edu.nupp.servetra.server.dao.SpecialistDao;
import ua.edu.nupp.servetra.server.error.ApiException;
import ua.edu.nupp.servetra.server.model.Specialist;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

// [ETAP 2] Specialist business service
// Requirement: Validate specialist identity and contact data before persistence.
public class SpecialistService {
    private static final Logger LOG = Logger.getLogger(SpecialistService.class.getName());
    private final SpecialistDao dao = new SpecialistDao();

    public List<Specialist> list() throws SQLException {
        return dao.findAll();
    }

    public Specialist get(long id) throws SQLException {
        return dao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Specialist was not found"));
    }

    public Specialist create(Specialist input) throws SQLException {
        Specialist value = validated(null, input);
        Specialist created = dao.create(value);
        LOG.info("Created specialist id=" + created.id());
        return created;
    }

    public Specialist update(long id, Specialist input) throws SQLException {
        get(id);
        Specialist value = validated(id, input);
        Specialist updated = dao.update(value);
        LOG.info("Updated specialist id=" + id);
        return updated;
    }

    public void delete(long id) throws SQLException {
        boolean deleted = dao.delete(id);
        if (!deleted) {
            throw ApiException.notFound("Specialist was not found");
        }
        LOG.info("Deleted specialist id=" + id);
    }

    public static Specialist validated(Long id, Specialist v) {
        if (v == null) {
            throw ApiException.validation("specialist body is required");
        }
        return new Specialist(
                id,
                Validation.required(v.fullName(), "fullName", 160),
                Validation.email(v.email()),
                Validation.optional(v.specialization(), "specialization", 120),
                Validation.optional(v.phone(), "phone", 40),
                v.active()
        );
    }
}


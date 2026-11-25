package ua.edu.nupp.servetra.server.dao;

import ua.edu.nupp.servetra.server.db.Db;
import ua.edu.nupp.servetra.server.model.ServiceRecord;
import ua.edu.nupp.servetra.server.model.WorkType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// [ETAP 2] JDBC persistence
// Requirement: Implement PreparedStatement CRUD for ServiceRecord.
public class ServiceRecordDao {
    public ServiceRecord create(ServiceRecord value) throws SQLException {
        String sql = "INSERT INTO service_record(request_id,specialist_id,work_type,description,performed_at,duration_minutes,cost_amount) VALUES(?,?,?,?,?,?,?)";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            bind(statement, value);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Service record id was not generated");
                return findById(keys.getLong(1)).orElseThrow();
            }
        }
    }

    public List<ServiceRecord> findAll(Long requestId) throws SQLException {
        String sql = "SELECT * FROM service_record" + (requestId == null ? "" : " WHERE request_id=?") + " ORDER BY performed_at DESC,id";
        List<ServiceRecord> result = new ArrayList<>();
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (requestId != null) {
                statement.setLong(1, requestId);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(map(rows));
                }
            }
        }
        return result;
    }

    public Optional<ServiceRecord> findById(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM service_record WHERE id=?"
             )) {
            statement.setLong(1, id);
            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    return Optional.of(map(row));
                }
                return Optional.empty();
            }
        }
    }

    public ServiceRecord update(ServiceRecord value) throws SQLException {
        String sql = "UPDATE service_record SET request_id=?,specialist_id=?,work_type=?,description=?,performed_at=?,duration_minutes=?,cost_amount=? WHERE id=?";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, value);
            statement.setLong(8, value.id());
            if (statement.executeUpdate() == 0) {
                return null;
            }
        }
        return findById(value.id()).orElse(null);
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM service_record WHERE id=?"
             )) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bind(PreparedStatement p, ServiceRecord v) throws SQLException {
        p.setLong(1, v.requestId());
        p.setLong(2, v.specialistId());
        p.setString(3, v.workType().name());
        p.setString(4, v.description());
        p.setTimestamp(5, Timestamp.valueOf(v.performedAt()));
        if (v.durationMinutes() == null) {
            p.setNull(6, Types.INTEGER);
        } else {
            p.setInt(6, v.durationMinutes());
        }
        if (v.costAmount() == null) {
            p.setNull(7, Types.DECIMAL);
        } else {
            p.setBigDecimal(7, v.costAmount());
        }
    }

    private static ServiceRecord map(ResultSet r) throws SQLException {
        int duration = r.getInt("duration_minutes");
        Integer durationMinutes = r.wasNull() ? null : duration;
        return new ServiceRecord(r.getLong("id"), r.getLong("request_id"), r.getLong("specialist_id"),
                WorkType.valueOf(r.getString("work_type")), r.getString("description"),
                r.getTimestamp("performed_at").toLocalDateTime(), durationMinutes, r.getBigDecimal("cost_amount"));
    }
}


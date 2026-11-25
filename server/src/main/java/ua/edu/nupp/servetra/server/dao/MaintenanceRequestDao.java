package ua.edu.nupp.servetra.server.dao;

import ua.edu.nupp.servetra.server.db.Db;
import ua.edu.nupp.servetra.server.model.MaintenanceRequest;
import ua.edu.nupp.servetra.server.model.RequestStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// [ETAP 2] JDBC persistence
// Requirement: Implement request CRUD and persisted lifecycle transitions.
public class MaintenanceRequestDao {
    public MaintenanceRequest create(MaintenanceRequest value) throws SQLException {
        String sql = "INSERT INTO maintenance_request(equipment_id,specialist_id,summary,description,diagnostic_notes,status,criticality_snapshot,severity_score,downtime_impact_score,priority_index,due_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = Db.open()) {
            connection.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    bindEditable(statement, value);
                    statement.setString(6, value.status().name());
                    statement.setInt(7, value.criticalitySnapshot());
                    statement.setInt(8, value.severityScore());
                    statement.setInt(9, value.downtimeImpactScore());
                    statement.setBigDecimal(10, value.priorityIndex());
                    setDateTime(statement, 11, value.dueAt());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Request id was not generated");
                        }
                        id = keys.getLong(1);
                    }
                }
                String number = "REQ-%06d".formatted(id);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE maintenance_request SET request_number=? WHERE id=?"
                )) {
                    statement.setString(1, number);
                    statement.setLong(2, id);
                    statement.executeUpdate();
                }
                connection.commit();
                return findById(id).orElseThrow();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<MaintenanceRequest> findAll(RequestStatus status) throws SQLException {
        String sql = "SELECT * FROM maintenance_request" + (status == null ? "" : " WHERE status=?") + " ORDER BY priority_index DESC,id";
        List<MaintenanceRequest> result = new ArrayList<>();
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (status != null) {
                statement.setString(1, status.name());
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(map(rows));
                }
            }
        }
        return result;
    }

    public Optional<MaintenanceRequest> findById(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM maintenance_request WHERE id=?"
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

    public MaintenanceRequest update(MaintenanceRequest value) throws SQLException {
        String sql = "UPDATE maintenance_request SET equipment_id=?,specialist_id=?,summary=?,description=?,diagnostic_notes=?,criticality_snapshot=?,severity_score=?,downtime_impact_score=?,priority_index=?,due_at=? WHERE id=?";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEditable(statement, value);
            statement.setInt(6, value.criticalitySnapshot());
            statement.setInt(7, value.severityScore());
            statement.setInt(8, value.downtimeImpactScore());
            statement.setBigDecimal(9, value.priorityIndex());
            setDateTime(statement, 10, value.dueAt());
            statement.setLong(11, value.id());
            if (statement.executeUpdate() == 0) {
                return null;
            }
        }
        return findById(value.id()).orElse(null);
    }

    public MaintenanceRequest transition(long id, RequestStatus next) throws SQLException {
        String sql = "UPDATE maintenance_request SET status=?,completed_at=? WHERE id=?";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, next.name());
            if (next == RequestStatus.COMPLETED) {
                statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                statement.setNull(2, Types.TIMESTAMP);
            }
            statement.setLong(3, id);
            if (statement.executeUpdate() == 0) {
                return null;
            }
        }
        return findById(id).orElse(null);
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM maintenance_request WHERE id=?"
             )) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bindEditable(PreparedStatement p, MaintenanceRequest v) throws SQLException {
        p.setLong(1, v.equipmentId());
        if (v.specialistId() == null) {
            p.setNull(2, Types.BIGINT);
        } else {
            p.setLong(2, v.specialistId());
        }
        p.setString(3, v.summary());
        p.setString(4, v.description());
        p.setString(5, v.diagnosticNotes());
    }

    private static void setDateTime(PreparedStatement p, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            p.setNull(index, Types.TIMESTAMP);
        } else {
            p.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static LocalDateTime dateTime(ResultSet r, String name) throws SQLException {
        Timestamp value = r.getTimestamp(name);
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }

    private static MaintenanceRequest map(ResultSet r) throws SQLException {
        long specialist = r.getLong("specialist_id");
        Long specialistId = r.wasNull() ? null : specialist;
        return new MaintenanceRequest(r.getLong("id"), r.getString("request_number"), r.getLong("equipment_id"),
                specialistId, r.getString("summary"), r.getString("description"), r.getString("diagnostic_notes"),
                RequestStatus.valueOf(r.getString("status")), r.getInt("criticality_snapshot"),
                r.getInt("severity_score"), r.getInt("downtime_impact_score"), r.getBigDecimal("priority_index"),
                dateTime(r, "due_at"), dateTime(r, "created_at"), dateTime(r, "updated_at"), dateTime(r, "completed_at"));
    }
}


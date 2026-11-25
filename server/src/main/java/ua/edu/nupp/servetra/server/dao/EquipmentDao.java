package ua.edu.nupp.servetra.server.dao;

import ua.edu.nupp.servetra.server.db.Db;
import ua.edu.nupp.servetra.server.model.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// [ETAP 2] JDBC persistence
// Requirement: Implement PreparedStatement CRUD for Equipment.
public class EquipmentDao {
    public Equipment create(Equipment value) throws SQLException {
        String sql = "INSERT INTO equipment(inventory_number,name,category,manufacturer,model,serial_number,location,department,criticality_score,next_service_due,active) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            bind(statement, value);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Equipment id was not generated");
                return findById(keys.getLong(1)).orElseThrow();
            }
        }
    }

    public List<Equipment> findAll(String query) throws SQLException {
        boolean filtered = query != null && !query.isBlank();
        String sql = "SELECT * FROM equipment" + (filtered ? " WHERE LOWER(inventory_number) LIKE ? OR LOWER(name) LIKE ? OR LOWER(location) LIKE ?" : "") + " ORDER BY id";
        List<Equipment> result = new ArrayList<>();
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filtered) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                statement.setString(1, pattern);
                statement.setString(2, pattern);
                statement.setString(3, pattern);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(map(rows));
                }
            }
        }
        return result;
    }

    public Optional<Equipment> findById(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM equipment WHERE id=?"
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

    public Equipment update(Equipment value) throws SQLException {
        String sql = "UPDATE equipment SET inventory_number=?,name=?,category=?,manufacturer=?,model=?,serial_number=?,location=?,department=?,criticality_score=?,next_service_due=?,active=? WHERE id=?";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, value);
            statement.setLong(12, value.id());
            if (statement.executeUpdate() == 0) {
                return null;
            }
        }
        return findById(value.id()).orElse(null);
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM equipment WHERE id=?"
             )) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bind(PreparedStatement p, Equipment v) throws SQLException {
        p.setString(1, v.inventoryNumber());
        p.setString(2, v.name());
        p.setString(3, v.category());
        p.setString(4, v.manufacturer());
        p.setString(5, v.model());
        p.setString(6, v.serialNumber());
        p.setString(7, v.location());
        p.setString(8, v.department());
        p.setInt(9, v.criticalityScore());
        if (v.nextServiceDue() == null) {
            p.setNull(10, Types.DATE);
        } else {
            p.setDate(10, Date.valueOf(v.nextServiceDue()));
        }
        p.setBoolean(11, v.active());
    }

    private static Equipment map(ResultSet r) throws SQLException {
        Date due = r.getDate("next_service_due");
        return new Equipment(r.getLong("id"), r.getString("inventory_number"), r.getString("name"),
                r.getString("category"), r.getString("manufacturer"), r.getString("model"),
                r.getString("serial_number"), r.getString("location"), r.getString("department"),
                r.getInt("criticality_score"), due == null ? null : due.toLocalDate(), r.getBoolean("active"));
    }
}


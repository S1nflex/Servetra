package ua.edu.nupp.servetra.server.dao;

import ua.edu.nupp.servetra.server.db.Db;
import ua.edu.nupp.servetra.server.model.Specialist;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// [ETAP 2] JDBC persistence
// Requirement: Implement PreparedStatement CRUD for Specialist.
public class SpecialistDao {
    public Specialist create(Specialist value) throws SQLException {
        String sql = "INSERT INTO specialist(full_name,email,specialization,phone,active) VALUES(?,?,?,?,?)";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            bind(statement, value);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Specialist id was not generated");
                return findById(keys.getLong(1)).orElseThrow();
            }
        }
    }

    public List<Specialist> findAll() throws SQLException {
        List<Specialist> result = new ArrayList<>();
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM specialist ORDER BY id"
             );
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(map(rows));
            }
        }
        return result;
    }

    public Optional<Specialist> findById(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM specialist WHERE id=?"
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

    public Specialist update(Specialist value) throws SQLException {
        String sql = "UPDATE specialist SET full_name=?,email=?,specialization=?,phone=?,active=? WHERE id=?";
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, value);
            statement.setLong(6, value.id());
            if (statement.executeUpdate() == 0) {
                return null;
            }
        }
        return findById(value.id()).orElse(null);
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = Db.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM specialist WHERE id=?"
             )) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bind(PreparedStatement p, Specialist v) throws SQLException {
        p.setString(1, v.fullName());
        p.setString(2, v.email());
        p.setString(3, v.specialization());
        p.setString(4, v.phone());
        p.setBoolean(5, v.active());
    }

    private static Specialist map(ResultSet r) throws SQLException {
        return new Specialist(r.getLong("id"), r.getString("full_name"), r.getString("email"),
                r.getString("specialization"), r.getString("phone"), r.getBoolean("active"));
    }
}


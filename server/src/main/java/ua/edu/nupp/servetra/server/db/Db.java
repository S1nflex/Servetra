package ua.edu.nupp.servetra.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// [ETAP 1] JDBC configuration
// Requirement: Open portable MySQL connections without hard-coded credentials.
public final class Db {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/servetra?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

    private Db() {}

    public static Connection open() throws SQLException {
        String url = env("SERVETRA_DB_URL", DEFAULT_URL);
        String user = env("SERVETRA_DB_USER", "servetra");
        String password = env("SERVETRA_DB_PASSWORD", "");
        DriverManager.setLoginTimeout(8);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}



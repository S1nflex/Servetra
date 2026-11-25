package ua.edu.nupp.servetra.server.servlet;

import ua.edu.nupp.servetra.server.db.Db;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

// [ETAP 3] Deployment health endpoint with a live JDBC check.
public class HealthServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (var connection = Db.open()) {
            String databaseStatus = connection.isValid(2) ? "UP" : "DOWN";
            ok(response, Map.of("status", "UP", "database", databaseStatus));
        } catch (Exception error) {
            fail(response, error);
        }
    }
}


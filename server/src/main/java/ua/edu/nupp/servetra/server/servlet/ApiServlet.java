package ua.edu.nupp.servetra.server.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.edu.nupp.servetra.server.error.ApiException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// [ETAP 3] HTTP/JSON integration
// Requirement: Centralize JSON serialization and safe HTTP error mapping.
public abstract class ApiServlet extends HttpServlet {
    protected final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Logger log = Logger.getLogger(getClass().getName());

    protected <T> T body(HttpServletRequest request, Class<T> type) throws IOException {
        return json.readValue(request.getInputStream(), type);
    }

    protected long id(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.equals("/") || path.isBlank()) {
            throw ApiException.badRequest("MISSING_ID", "Resource id is required");
        }
        String first = path.substring(1).split("/")[0];
        try {
            return Long.parseLong(first);
        } catch (NumberFormatException error) {
            throw ApiException.badRequest("INVALID_ID", "Resource id is invalid");
        }
    }

    protected boolean collection(HttpServletRequest request) {
        return request.getPathInfo() == null || request.getPathInfo().equals("/");
    }

    protected void ok(HttpServletResponse response, Object value) throws IOException {
        write(response, 200, value);
    }

    protected void created(HttpServletResponse response, Object value) throws IOException {
        write(response, 201, value);
    }

    protected void noContent(HttpServletResponse response) {
        response.setStatus(204);
    }

    protected void write(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getWriter(), value);
    }

    protected void fail(HttpServletResponse response, Exception error) throws IOException {
        if (error instanceof JsonProcessingException) {
            writeError(response, 400, "BAD_JSON", "Request JSON is invalid");
            return;
        }
        if (error instanceof ApiException api) {
            log.warning(() -> "Rejected API request: code=" + api.code() + ", message=" + api.getMessage());
            writeError(response, api.status(), api.code(), api.getMessage());
            return;
        }
        if (error instanceof SQLIntegrityConstraintViolationException) {
            writeError(
                    response,
                    409,
                    "DATA_CONFLICT",
                    "The value is duplicated or the record is referenced"
            );
            return;
        }
        if (error instanceof SQLException) {
            log.log(Level.SEVERE, "Database operation failed", error);
            writeError(response, 503, "DATABASE_UNAVAILABLE", "Database operation failed");
            return;
        }
        if (error instanceof IllegalArgumentException) {
            log.warning(() -> "Rejected malformed API request: " + error.getClass().getSimpleName());
            writeError(response, 400, "INVALID_REQUEST", "Request data is invalid");
            return;
        }
        log.log(Level.SEVERE, "Unexpected server error", error);
        writeError(response, 500, "INTERNAL_ERROR", "Internal server error");
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        Map<String, String> details = Map.of("code", code, "message", message);
        write(response, status, Map.of("error", details));
    }
}


package ua.edu.nupp.servetra.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.edu.nupp.servetra.client.model.Models.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

// [ETAP 3] HTTP/JSON client. The JavaFX module has no JDBC dependency.
public class ApiClient {
    private final String base;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public ApiClient() {
        this(System.getenv().getOrDefault(
                "SERVETRA_API_BASE",
                "http://localhost:8080/servetra-server/api/v1"
        ));
    }

    public ApiClient(String base) {
        if (base.endsWith("/")) {
            this.base = base.substring(0, base.length() - 1);
        } else {
            this.base = base;
        }
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public List<Equipment> equipment(String query) {
        String path = "/equipment";
        if (query != null && !query.isBlank()) {
            path = path + "?q=" + encode(query);
        }
        String response = send("GET", path, null);
        return read(response, new TypeReference<>() {});
    }

    public Equipment createEquipment(Equipment value) {
        return read(send("POST", "/equipment", value), Equipment.class);
    }

    public Equipment updateEquipment(long id, Equipment value) {
        return read(send("PUT", "/equipment/" + id, value), Equipment.class);
    }

    public void deleteEquipment(long id) {
        send("DELETE", "/equipment/" + id, null);
    }

    public List<Specialist> specialists() {
        String response = send("GET", "/specialists", null);
        return read(response, new TypeReference<>() {});
    }

    public Specialist createSpecialist(Specialist value) {
        return read(send("POST", "/specialists", value), Specialist.class);
    }

    public Specialist updateSpecialist(long id, Specialist value) {
        return read(send("PUT", "/specialists/" + id, value), Specialist.class);
    }

    public void deleteSpecialist(long id) {
        send("DELETE", "/specialists/" + id, null);
    }

    public List<MaintenanceRequest> requests(RequestStatus status) {
        String path = "/requests";
        if (status != null) {
            path = path + "?status=" + status;
        }
        String response = send("GET", path, null);
        return read(response, new TypeReference<>() {});
    }

    public MaintenanceRequest createRequest(MaintenanceRequest value) {
        return read(send("POST", "/requests", value), MaintenanceRequest.class);
    }

    public MaintenanceRequest updateRequest(long id, MaintenanceRequest value) {
        return read(send("PUT", "/requests/" + id, value), MaintenanceRequest.class);
    }

    public MaintenanceRequest transition(long id, RequestStatus status) {
        String path = "/requests/" + id + "/transitions";
        return read(send("POST", path, new Transition(status)), MaintenanceRequest.class);
    }

    public void deleteRequest(long id) {
        send("DELETE", "/requests/" + id, null);
    }

    public List<ServiceRecord> records(Long requestId) {
        String path = "/service-records";
        if (requestId != null) {
            path = path + "?requestId=" + requestId;
        }
        String response = send("GET", path, null);
        return read(response, new TypeReference<>() {});
    }

    public ServiceRecord createRecord(ServiceRecord value) {
        return read(send("POST", "/service-records", value), ServiceRecord.class);
    }

    public ServiceRecord updateRecord(long id, ServiceRecord value) {
        return read(send("PUT", "/service-records/" + id, value), ServiceRecord.class);
    }

    public void deleteRecord(long id) {
        send("DELETE", "/service-records/" + id, null);
    }

    private record Transition(RequestStatus toStatus) {
    }

    private String send(String method, String path, Object value) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json");
            setBody(builder, method, value);

            HttpRequest request = builder.build();
            HttpResponse<String> response = http.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() >= 400) {
                throw apiError(response.statusCode(), response.body());
            }
            return response.body();
        } catch (ApiClientException error) {
            throw error;
        } catch (Exception error) {
            String message = error.getMessage();
            if (message == null) {
                message = "Network error";
            }
            throw new ApiClientException(0, "NETWORK_ERROR", message);
        }
    }

    private void setBody(HttpRequest.Builder builder, String method, Object value) throws Exception {
        if (value == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
            return;
        }

        String body = json.writeValueAsString(value);
        builder.header("Content-Type", "application/json; charset=UTF-8");
        builder.method(
                method,
                HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
        );
    }

    private ApiClientException apiError(int status, String body) {
        try {
            JsonNode error = json.readTree(body).path("error");
            String code = error.path("code").asText("HTTP_ERROR");
            String message = error.path("message").asText("HTTP " + status);
            return new ApiClientException(status, code, message);
        } catch (Exception ignored) {
            return new ApiClientException(status, "HTTP_ERROR", "HTTP " + status);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception error) {
            throw new ApiClientException(0, "JSON_ERROR", "Invalid server response");
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception error) {
            throw new ApiClientException(0, "JSON_ERROR", "Invalid server response");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

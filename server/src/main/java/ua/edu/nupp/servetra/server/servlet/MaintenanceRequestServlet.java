package ua.edu.nupp.servetra.server.servlet;

import ua.edu.nupp.servetra.server.model.MaintenanceRequest;
import ua.edu.nupp.servetra.server.model.RequestStatus;
import ua.edu.nupp.servetra.server.service.MaintenanceRequestService;

import javax.servlet.http.*;
import java.io.IOException;

// [ETAP 3] HTTP/JSON resource endpoint for request CRUD and lifecycle transitions.
public class MaintenanceRequestServlet extends ApiServlet {
    private final MaintenanceRequestService service = new MaintenanceRequestService();

    public record TransitionCommand(RequestStatus toStatus) {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (collection(request)) {
                String value = request.getParameter("status");
                RequestStatus status = null;
                if (value != null && !value.isBlank()) {
                    status = RequestStatus.valueOf(value);
                }
                ok(response, service.list(status));
            } else {
                ok(response, service.get(id(request)));
            }
        } catch (Exception error) {
            fail(response, error);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String path = request.getPathInfo();
            if (path != null && path.endsWith("/transitions")) {
                TransitionCommand command = body(request, TransitionCommand.class);
                ok(response, service.transition(id(request), command.toStatus()));
            } else {
                MaintenanceRequest value = body(request, MaintenanceRequest.class);
                created(response, service.create(value));
            }
        } catch (Exception error) {
            fail(response, error);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            MaintenanceRequest value = body(request, MaintenanceRequest.class);
            ok(response, service.update(id(request), value));
        } catch (Exception error) {
            fail(response, error);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            service.delete(id(request));
            noContent(response);
        } catch (Exception error) {
            fail(response, error);
        }
    }
}


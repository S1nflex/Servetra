package ua.edu.nupp.servetra.server.servlet;

import ua.edu.nupp.servetra.server.model.ServiceRecord;
import ua.edu.nupp.servetra.server.service.ServiceRecordService;

import javax.servlet.http.*;
import java.io.IOException;

// [ETAP 3] HTTP/JSON resource endpoint for ServiceRecord CRUD.
public class ServiceRecordServlet extends ApiServlet {
    private final ServiceRecordService service = new ServiceRecordService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (collection(request)) {
                String value = request.getParameter("requestId");
                Long requestId = null;
                if (value != null && !value.isBlank()) {
                    requestId = Long.valueOf(value);
                }
                ok(response, service.list(requestId));
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
            ServiceRecord value = body(request, ServiceRecord.class);
            created(response, service.create(value));
        } catch (Exception error) {
            fail(response, error);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ServiceRecord value = body(request, ServiceRecord.class);
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


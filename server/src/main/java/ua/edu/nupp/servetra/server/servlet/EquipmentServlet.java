package ua.edu.nupp.servetra.server.servlet;

import ua.edu.nupp.servetra.server.model.Equipment;
import ua.edu.nupp.servetra.server.service.EquipmentService;

import javax.servlet.http.*;
import java.io.IOException;

// [ETAP 3] HTTP/JSON resource endpoint for Equipment CRUD.
public class EquipmentServlet extends ApiServlet {
    private final EquipmentService service = new EquipmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (collection(request)) {
                String query = request.getParameter("q");
                ok(response, service.list(query));
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
            Equipment value = body(request, Equipment.class);
            created(response, service.create(value));
        } catch (Exception error) {
            fail(response, error);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Equipment value = body(request, Equipment.class);
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


package ua.edu.nupp.servetra.server.servlet;

import ua.edu.nupp.servetra.server.model.Specialist;
import ua.edu.nupp.servetra.server.service.SpecialistService;

import javax.servlet.http.*;
import java.io.IOException;

// [ETAP 3] HTTP/JSON resource endpoint for Specialist CRUD.
public class SpecialistServlet extends ApiServlet {
    private final SpecialistService service = new SpecialistService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (collection(request)) {
                ok(response, service.list());
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
            Specialist value = body(request, Specialist.class);
            created(response, service.create(value));
        } catch (Exception error) {
            fail(response, error);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Specialist value = body(request, Specialist.class);
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


package guessmarket.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class JsonServlet extends HttpServlet {

    @Override
    protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            handleGet(request, response);
        } catch (IllegalArgumentException e) {
            JsonResponses.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonResponses.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected final void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            handlePost(request, response);
        } catch (IllegalArgumentException e) {
            JsonResponses.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            JsonResponses.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected void handleGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        JsonResponses.writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET not supported here.");
    }

    protected void handlePost(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        JsonResponses.writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST not supported here.");
    }
}

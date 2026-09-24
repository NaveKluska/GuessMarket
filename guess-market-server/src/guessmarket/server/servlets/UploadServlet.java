package guessmarket.server.servlets;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.server.JsonResponses;
import guessmarket.server.JsonServlet;
import guessmarket.server.ServerState;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@WebServlet("/upload")
public class UploadServlet extends JsonServlet {

    @Override
    protected void handlePost(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final MarketEngine engine = ServerState.getEngine();
        final String uploaderName = request.getParameter("uploaderName");
        engine.addEventsFromUpload(uploaderName, request.getInputStream());
        JsonResponses.writeSuccess(response, Map.of("status", "ok"));
    }
}

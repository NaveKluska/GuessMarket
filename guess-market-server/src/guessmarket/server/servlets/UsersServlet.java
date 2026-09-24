package guessmarket.server.servlets;

import com.google.gson.JsonObject;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.server.JsonResponses;
import guessmarket.server.JsonServlet;
import guessmarket.server.Requests;
import guessmarket.server.ServerState;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@WebServlet("/users/*")
public class UsersServlet extends JsonServlet {

    @Override
    protected void handleGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        JsonResponses.writeSuccess(response, ServerState.getEngine().getAllUsers());
    }

    @Override
    protected void handlePost(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final MarketEngine engine = ServerState.getEngine();
        final String action = request.getPathInfo();
        final JsonObject body = Requests.readJsonBody(request);

        if ("/register".equals(action)) {
            engine.registerUser(body.get("name").getAsString());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else if ("/deposit".equals(action)) {
            engine.depositCash(body.get("userName").getAsString(), body.get("amount").getAsDouble());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else {
            JsonResponses.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown action: " + action);
        }
    }
}

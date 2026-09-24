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

@WebServlet("/chat")
public class ChatServlet extends JsonServlet {

    @Override
    protected void handleGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        JsonResponses.writeSuccess(response, ServerState.getEngine().getChatMessages());
    }

    @Override
    protected void handlePost(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final MarketEngine engine = ServerState.getEngine();
        final JsonObject body = Requests.readJsonBody(request);
        engine.postChatMessage(body.get("userName").getAsString(), body.get("message").getAsString());
        JsonResponses.writeSuccess(response, Map.of("status", "ok"));
    }
}

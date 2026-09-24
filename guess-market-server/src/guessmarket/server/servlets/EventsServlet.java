package guessmarket.server.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import guessmarket.dto.ReceiptDTO;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.orderbook.OrderSide;
import guessmarket.server.JsonResponses;
import guessmarket.server.JsonServlet;
import guessmarket.server.Requests;
import guessmarket.server.ServerState;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/events/*")
public class EventsServlet extends JsonServlet {

    @Override
    protected void handleGet(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final MarketEngine engine = ServerState.getEngine();
        final String name = request.getParameter("name");

        if (name == null) {
            JsonResponses.writeSuccess(response, engine.getAllEvents());
        } else {
            JsonResponses.writeSuccess(response, engine.getEventDetails(name));
        }
    }

    @Override
    protected void handlePost(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final MarketEngine engine = ServerState.getEngine();
        final String action = request.getPathInfo();
        final JsonObject body = Requests.readJsonBody(request);

        if ("/open".equals(action)) {
            engine.openEvent(body.get("mmName").getAsString(), body.get("eventName").getAsString());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else if ("/close".equals(action)) {
            engine.closeEvent(body.get("mmName").getAsString(), body.get("eventName").getAsString(), body.get("winningOptionIndex").getAsInt());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else if ("/buy".equals(action)) {
            final ReceiptDTO receipt = engine.buyShares(body.get("memberName").getAsString(), body.get("eventName").getAsString(), body.get("optionIndex").getAsInt(), body.get("quantity").getAsInt());
            JsonResponses.writeSuccess(response, receipt);
        } else if ("/order".equals(action)) {
            engine.submitOrder(body.get("userName").getAsString(), body.get("eventName").getAsString(), body.get("optionIndex").getAsInt(), OrderSide.valueOf(body.get("side").getAsString()), body.get("price").getAsDouble(), body.get("quantity").getAsInt());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else if ("/create-lmsr".equals(action)) {
            engine.createLmsrEvent(body.get("creatorName").getAsString(), body.get("name").getAsString(), body.get("description").getAsString(), body.get("commission").getAsInt(), CommissionType.valueOf(body.get("commissionType").getAsString()), toStringList(body.get("optionNames").getAsJsonArray()), body.get("b").getAsInt());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else if ("/create-orderbook".equals(action)) {
            engine.createOrderBookEvent(body.get("creatorName").getAsString(), body.get("name").getAsString(), body.get("description").getAsString(), body.get("commission").getAsInt(), CommissionType.valueOf(body.get("commissionType").getAsString()), toStringList(body.get("optionNames").getAsJsonArray()), body.get("allowMint").getAsBoolean(), body.get("initial").getAsInt(), body.get("d").getAsInt());
            JsonResponses.writeSuccess(response, Map.of("status", "ok"));
        } else {
            JsonResponses.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown action: " + action);
        }
    }

    private List<String> toStringList(final JsonArray array) {
        final List<String> result = new ArrayList<>();
        for (final JsonElement element : array) {
            result.add(element.getAsString());
        }
        return result;
    }
}

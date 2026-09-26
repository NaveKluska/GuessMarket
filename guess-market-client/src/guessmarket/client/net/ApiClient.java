package guessmarket.client.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import guessmarket.dto.ChatMessageDTO;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.UserSummaryDTO;
import guessmarket.dto.lmsr.LmsrEventDetailsDTO;
import guessmarket.dto.orderbook.OrderBookEventDetailsDTO;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Every request the client makes to the server goes through here - one method per MarketEngine
 * operation, same order as MarketEngine itself. Nothing here talks to the engine directly; it
 * only ever builds an HTTP request and parses the JSON that comes back.
 */
public final class ApiClient
{
    private static final String BASE_URL = "http://localhost:8080/guessmarket";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final MediaType BINARY_MEDIA = MediaType.get("application/octet-stream");

    private static final OkHttpClient http = new OkHttpClient();

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
            LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .registerTypeAdapter(EventDetailsDTO.class, (JsonDeserializer<EventDetailsDTO>) (json, type, context) -> {
            if (json.getAsJsonObject().has("options")) {
                return context.deserialize(json, LmsrEventDetailsDTO.class);
            }
            return context.deserialize(json, OrderBookEventDetailsDTO.class);
        })
        .create();

    private ApiClient()
    {
    }

    public static List<EventSummaryDTO> getAllEvents()
    {
        final String body = get("/events");
        return gson.fromJson(body, new TypeToken<List<EventSummaryDTO>>() {}.getType());
    }

    public static void addEventsFromUpload(final String uploaderName, final byte[] fileContent)
    {
        postRaw("/upload?uploaderName=" + urlEncode(uploaderName), fileContent);
    }

    public static List<UserSummaryDTO> getAllUsers()
    {
        final String body = get("/users");
        return gson.fromJson(body, new TypeToken<List<UserSummaryDTO>>() {}.getType());
    }

    public static void registerUser(final String name)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        post("/users/register", payload);
    }

    public static void depositCash(final String userName, final double amount)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("userName", userName);
        payload.addProperty("amount", amount);
        post("/users/deposit", payload);
    }

    public static EventDetailsDTO getEventDetails(final String eventName)
    {
        final String body = get("/events?name=" + urlEncode(eventName));
        return gson.fromJson(body, EventDetailsDTO.class);
    }

    public static ReceiptDTO buyShares(final String memberName, final String eventName, final int optionIndex, final int quantity)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("memberName", memberName);
        payload.addProperty("eventName", eventName);
        payload.addProperty("optionIndex", optionIndex);
        payload.addProperty("quantity", quantity);
        final String body = post("/events/buy", payload);
        return gson.fromJson(body, ReceiptDTO.class);
    }

    public static void openEvent(final String mmName, final String eventName)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("mmName", mmName);
        payload.addProperty("eventName", eventName);
        post("/events/open", payload);
    }

    public static void closeEvent(final String mmName, final String eventName, final int winningOptionIndex)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("mmName", mmName);
        payload.addProperty("eventName", eventName);
        payload.addProperty("winningOptionIndex", winningOptionIndex);
        post("/events/close", payload);
    }

    public static void submitOrder(final String userName, final String eventName, final int optionIndex, final OrderSide side, final double price, final int quantity)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("userName", userName);
        payload.addProperty("eventName", eventName);
        payload.addProperty("optionIndex", optionIndex);
        payload.addProperty("side", side.name());
        payload.addProperty("price", price);
        payload.addProperty("quantity", quantity);
        post("/events/order", payload);
    }

    public static void createLmsrEvent(final String creatorName, final String name, final String description, final int commission, final CommissionType commissionType, final List<String> optionNames, final int b)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("creatorName", creatorName);
        payload.addProperty("name", name);
        payload.addProperty("description", description);
        payload.addProperty("commission", commission);
        payload.addProperty("commissionType", commissionType.name());
        payload.add("optionNames", gson.toJsonTree(optionNames));
        payload.addProperty("b", b);
        post("/events/create-lmsr", payload);
    }

    public static void createOrderBookEvent(final String creatorName, final String name, final String description, final int commission, final CommissionType commissionType, final List<String> optionNames, final boolean allowMint, final int initial, final int d)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("creatorName", creatorName);
        payload.addProperty("name", name);
        payload.addProperty("description", description);
        payload.addProperty("commission", commission);
        payload.addProperty("commissionType", commissionType.name());
        payload.add("optionNames", gson.toJsonTree(optionNames));
        payload.addProperty("allowMint", allowMint);
        payload.addProperty("initial", initial);
        payload.addProperty("d", d);
        post("/events/create-orderbook", payload);
    }

    public static void postChatMessage(final String userName, final String message)
    {
        final JsonObject payload = new JsonObject();
        payload.addProperty("userName", userName);
        payload.addProperty("message", message);
        post("/chat", payload);
    }

    public static List<ChatMessageDTO> getChatMessages()
    {
        final String body = get("/chat");
        return gson.fromJson(body, new TypeToken<List<ChatMessageDTO>>() {}.getType());
    }

    // ============================================================================================
    // Helpers
    // ============================================================================================

    private static String get(final String path)
    {
        final Request request = new Request.Builder().url(BASE_URL + path).get().build();
        return execute(request);
    }

    private static String post(final String path, final JsonObject payload)
    {
        final RequestBody requestBody = RequestBody.create(payload.toString(), JSON_MEDIA);
        final Request request = new Request.Builder().url(BASE_URL + path).post(requestBody).build();
        return execute(request);
    }

    private static String postRaw(final String path, final byte[] content)
    {
        final RequestBody requestBody = RequestBody.create(content, BINARY_MEDIA);
        final Request request = new Request.Builder().url(BASE_URL + path).post(requestBody).build();
        return execute(request);
    }

    private static String execute(final Request request)
    {
        try (Response response = http.newCall(request).execute()) {
            final String text = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new ApiException(extractErrorMessage(text, response.code()));
            }
            return text;
        } catch (final IOException e) {
            throw new ApiException("Could not reach the server: " + e.getMessage());
        }
    }

    private static String extractErrorMessage(final String text, final int statusCode)
    {
        try {
            final JsonObject object = JsonParser.parseString(text).getAsJsonObject();
            if (object.has("error")) {
                return object.get("error").getAsString();
            }
        } catch (final Exception ignored) {
            // Not a JSON error body - e.g. a container-level error page (404/500) that never
            // reached the servlet. Fall through to a generic message instead of dumping raw HTML.
        }
        return "Server returned an unexpected error (HTTP " + statusCode + ").";
    }

    private static String urlEncode(final String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

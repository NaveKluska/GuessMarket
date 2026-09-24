package guessmarket.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;

public final class Requests {

    private Requests() {
    }

    public static JsonObject readJsonBody(final HttpServletRequest request) throws IOException {
        final StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return JsonParser.parseString(body.toString()).getAsJsonObject();
    }
}

package com.example.teambalancer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiErrorReader {

    private ApiErrorReader() {}

    public static String readMessage(Response<?> response) {
        ResponseBody err = response.errorBody();
        if (err == null) {
            return "HTTP " + response.code();
        }
        try {
            String raw = err.string();
            JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
            if (o.has("error") && !o.get("error").isJsonNull()) {
                return o.get("error").getAsString();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "HTTP " + response.code();
    }

    public static String readIoMessage(IOException e) {
        String m = e.getMessage();
        return m != null ? m : "Network error";
    }
}

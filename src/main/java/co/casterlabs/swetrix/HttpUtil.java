package co.casterlabs.swetrix;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.NonNull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class HttpUtil {
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType applicationJson = MediaType.parse("application/json");

    static @Nullable JsonObject post(@NonNull String url, @NonNull JsonObject body) throws IOException {
        try (Response response = client.newCall(
            new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), applicationJson))
                .build()
        ).execute()) {
            String responseBody = response.body().string();

            if (responseBody.isEmpty()) {
                return null;
            }

            return Rson.DEFAULT.fromJson(responseBody, JsonObject.class);
        }
    }

}

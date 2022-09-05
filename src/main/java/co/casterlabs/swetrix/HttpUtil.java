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
    private static final String userAgent;

    static {
        // The following device agents are just here to get Swetrix to count the device
        // type properly. They just need to *vaguely* represent the device.
        String device = "";

        if (System.getProperty("java.specification.vendor", "").equals("The Android Project")) {
            device = "(Linux; U; Android 0.0.0) Mobile ";
        } else {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.matches("mac|darwin")) {
                device = "(Macintosh; Intel Mac OS X 10_15_7) ";
            } else if (osName.matches("nux")) {
                device = "(X11; Linux; Linux x86_64; rv:15.0) ";
            } else if (osName.matches("win")) {
                device = "(Windows NT 10.0; Win64; x64) ";
            }
        }

        userAgent = String.format("%sSwetrix; Casterlabs-SDK 1.0.0", device);
    }

    static @Nullable JsonObject post(@NonNull String url, @NonNull JsonObject body) throws IOException {
        try (Response response = client.newCall(
            new Request.Builder()
                .header("User-Agent", userAgent)
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

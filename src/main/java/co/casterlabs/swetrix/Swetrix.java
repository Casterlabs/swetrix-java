package co.casterlabs.swetrix;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.With;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Swetrix {
    private FastLogger logger;
    private Builder config;

    static {
        Swetrix.class.getClassLoader().setDefaultAssertionStatus(true);
    }

    public Swetrix(@NonNull String projectId) {
        this(new Builder(projectId));
    }

    private Swetrix(Builder config) {
        this.config = config;
        this.logger = new FastLogger(String.format("Swetrix (%s)", this.config.projectId));

        this.logger.setCurrentLevel(this.config.debugEnabled ? LogLevel.DEBUG : LogLevel.WARNING);

        if (this.config.debugEnabled) {
            this.logger.debug("Debug mode enabled! Using config: %s", this.config);
        }
    }

    public void track(@NonNull String event, boolean unique) {
        if (this.config.analyticsDisabled) {
            this.logger.debug("Analytics are disabled, not sending track request.");
            return;
        }

        try {
            JsonObject response = HttpUtil.post(
                this.config.apiUrl + "/custom",
                new JsonObject()
                    .put("pid", this.config.projectId)
                    .put("ev", event)
                    .put("unique", unique)
            );

            if (response == null) {
                // All good!
                this.logger.debug("Successfully tracked event \"%s\" (unique: %b)", event, unique);
                return;
            }

            String error = response.getString("error");
            String errorMessage = response.getString("message");

            if (errorMessage.contains("unique option provided")) {
                this.logger.warn("Already tracked unique event \"%s\" for session.", event);
            } else {
                this.logger.severe("An API error occurred:\n%s: %s", error, errorMessage);
            }
        } catch (IOException e) {
            this.logger.severe("An error occurred whilst making API call:\n%s", e);
        }
    }

    /* ---------------- */
    /* Builder Stuff    */
    /* ---------------- */

    public static Builder builder(@NonNull String projectId) {
        return new Builder(projectId);
    }

    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private final @With boolean debugEnabled;
        private final @With boolean analyticsDisabled;
        private final @With String apiUrl;
        private final String projectId;

        private Builder(@NonNull String projectId) {
            this.debugEnabled = false;
            this.analyticsDisabled = false;
            this.apiUrl = "https://api.swetrix.com/log";
            this.projectId = projectId;
        }

        public Swetrix build() {
            assert !this.apiUrl.isEmpty() : "Api URL cannot be empty.";
            assert !this.projectId.isEmpty() : "Project ID cannot be empty.";
            return new Swetrix(this);
        }

    }

}

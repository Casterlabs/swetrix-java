package co.casterlabs.swetrix;

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

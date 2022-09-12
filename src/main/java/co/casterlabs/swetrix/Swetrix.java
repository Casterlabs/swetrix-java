package co.casterlabs.swetrix;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.With;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Swetrix {
    private static final long HEARTBEAT_INTERVAL = TimeUnit.SECONDS.toMillis(25);

    private FastLogger logger;
    private Builder config;

    private Thread heartbeatThread;

    private boolean hasStartedSession = false;

    static {
        Swetrix.class.getClassLoader().setDefaultAssertionStatus(true);
    }

    /**
     * Instantiates a new Swetrix Analytics instance.
     *
     * @param projectId the project id
     * 
     * @see             #builder(String)
     */
    public Swetrix(@NonNull String projectId) {
        this(new Builder(projectId));
    }

    private Swetrix(Builder config) {
        this.config = config;
        this.logger = new FastLogger(String.format("Swetrix (%s)", this.config.projectId));

        this.logger.setCurrentLevel(this.config.debugEnabled ? LogLevel.DEBUG : LogLevel.WARNING);

        if (this.config.debugEnabled) {
            this.logger.debug("Debug mode enabled! Using config: %s", this.config);
            this.logger.debug("User agent: %s", HttpUtil.userAgent);
        }
    }

    /* ---------------- */
    /* Tracking         */
    /* ---------------- */

    /**
     * Track a custom event.
     * 
     * @apiNote        In order for this event to be counted correctly, you must
     *                 call {@link #trackPageView(String)} or
     *                 {@link #trackPageView(String, String)} for the user session
     *                 to be started.
     * 
     * @param   event  the event name, must be alphanumerical (with _).
     * @param   unique whether or not this event is unique. Unique events do not get
     *                 counted additional times.
     */
    public void track(@NonNull String event, boolean unique) {
        assert this.hasStartedSession : "You must call trackPageView() first in order for the user session to be created.";

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

    /**
     * Tracks a page view. This method automatically figures out the locale in
     * contrast {@link #trackPageView(String, String)}.
     *
     * @param page the page
     */
    public void trackPageView(@NonNull String page) {
        Locale locale = Locale.getDefault();
        String lc = locale.toLanguageTag();

        this.trackPageView(page, lc);
    }

    /**
     * Tracks a page view.
     * 
     *
     * @param page   the page
     * @param locale the locale of the user (e.g en-US)
     * 
     * @see          #trackPageView(String)
     */
    public void trackPageView(@NonNull String page, @NonNull String locale) {
        this.hasStartedSession = true;

        if (this.config.analyticsDisabled) {
            this.logger.debug("Analytics are disabled, not sending trackPageView request.");
            return;
        }

        String timeZone = TimeZone.getDefault().getID();

        try {
            JsonObject response = HttpUtil.post(
                this.config.apiUrl,
                new JsonObject()
                    .put("pid", this.config.projectId)
                    .put("pg", page)
                    .put("tz", timeZone)
                    .put("lc", locale)
                    .put("unique", false)
            );

            if (response == null) {
                // All good!
                this.logger.debug("Successfully tracked page view \"%s\" (tz: %s, lc: %s)", page, timeZone, locale);
                return;
            }

            String error = response.getString("error");
            String errorMessage = response.getString("message");

            this.logger.severe("An API error occurred:\n%s: %s", error, errorMessage);
        } catch (IOException e) {
            this.logger.severe("An error occurred whilst making API call:\n%s", e);
        }
    }

    /* ---------------- */
    /* Heartbeat        */
    /* ---------------- */

    private void sendHB() {
        if (this.config.analyticsDisabled) {
            this.logger.debug("Analytics are disabled, not sending heartbeat.");
            return;
        }

        try {
            JsonObject response = HttpUtil.post(
                this.config.apiUrl + "/hb",
                new JsonObject()
                    .put("pid", this.config.projectId)
            );

            if (response == null) {
                // All good!
                this.logger.debug("Successfully sent heartbeat event.");
                return;
            }

            String error = response.getString("error");
            String errorMessage = response.getString("message");

            this.logger.severe("An API error occurred:\n%s: %s", error, errorMessage);
        } catch (IOException e) {
            this.logger.severe("An error occurred whilst making API call:\n%s", e);
        }
    }

    /**
     * Starts the heartbeat signal. This is for the "Live visitors" statistic and
     * must be started manually by you.
     * 
     * @apiNote In order for this event to be counted correctly, you must call
     *          {@link #trackPageView(String)} or
     *          {@link #trackPageView(String, String)} for the client session to be
     *          started.
     * 
     * @see     #stopHeartbeat()
     */
    public void startHeartbeat() {
        assert this.hasStartedSession : "You must call trackPageView() first in order for the user session to be created.";

        if (this.heartbeatThread != null) return;

        this.heartbeatThread = new Thread(() -> {
            Thread current = Thread.currentThread();

            while (!current.isInterrupted()) {
                this.sendHB();

                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (InterruptedException e) {}
            }
        });
        this.heartbeatThread.setName("Swetrix Heartbeat Thread");
        this.heartbeatThread.setPriority(Thread.MIN_PRIORITY);
        this.heartbeatThread.setDaemon(true);
        this.heartbeatThread.start();
    }

    /**
     * Stops the hearbeat signal.
     * 
     * @see #startHeartbeat()
     */
    public void stopHeartbeat() {
        if (this.heartbeatThread == null) return;

        this.heartbeatThread.interrupt();
        this.heartbeatThread = null;
    }

    /* ---------------- */
    /* Builder          */
    /* ---------------- */

    /**
     * Creates a new chainable builder for creating a new Swetrix analytics
     * instance.
     *
     * @param  projectId the project id
     * 
     * @return           a builder instance.
     */
    public static Builder builder(@NonNull String projectId) {
        return new Builder(projectId);
    }

    @ToString
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        /** Whether or not debug logs should be printed to console. */
        private final @With boolean debugEnabled;

        /**
         * Whether or not to disable analytics tracking, useful for debugging/dev
         * environments.
         */
        private final @With boolean analyticsDisabled;

        /** The API URL to use, useful for self-hosted instances. */
        private final @With String apiUrl;

        private final String projectId;

        private Builder(@NonNull String projectId) {
            this.debugEnabled = false;
            this.analyticsDisabled = false;
            this.apiUrl = "https://api.swetrix.com/log";
            this.projectId = projectId;
        }

        /**
         * Builds a new Swetrix analytics instance based off the settings you've set.
         *
         * @return a new Swetrix analytics instance.
         */
        public Swetrix build() {
            assert !this.apiUrl.isEmpty() : "API URL cannot be empty.";
            assert !this.projectId.isEmpty() : "Project ID cannot be empty.";
            return new Swetrix(this);
        }

    }

}

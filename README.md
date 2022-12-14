# swetrix-java

## Usage
```java
Swetrix swetrix = Swetrix.builder("YOUR_PROJECT_ID")
    .withDebugEnabled(true)       // Useful for debugging.
    .withAnalyticsDisabled(false) // For local/dev environments.
    .withApiUrl("https://...")    // If you have your own self-hosted instance. Omit this to use the default Swetrix API.
    .build();

// Or, if you just want an quick instance with defaults, new Swetrix("YOUR_PROJECT_ID");

swetrix.trackPageView("/"); // Tracking page views is easy.

swetrix.track("my_custom_event", false); // You can also track custom events.

swetrix.startHeartbeat(); // This updates your "Live visitors" statistic in Swetrix. 
```

## Installation (Maven)
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>co.casterlabs</groupId>
            <artifactId>swetrix-java</artifactId>
            <version>LATEST_VERSION_HERE</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```

## TODO
 - [x] Page View Tracking
 - [x] Custom Event Tracking
 - [x] Heartbeats

package rocks.inspectit.ocelot.config.utils;

/**
 * Utility class for OpenTelemetry endpoints.
 */
public class EndpointUtils {

    /**
     * Transforms the given {@code endpoint} to meet OTEL's requirement to start with either 'http://' or 'https://'.
     * E.g., the {@code endpoint} 'localhost:4317' will be returned as 'http://localhost:4317'.
     * If the {@code endpoint} starts with 'http://' or 'https://', it will be returned unchanged.
     *
     * @param endpoint
     *
     * @return The padded endpoint
     */
    public static String padEndpoint(String endpoint) {
        return endpoint == null || endpoint.isEmpty() || endpoint.startsWith("http") ? endpoint : String.format("http://%s", endpoint);
    }
}

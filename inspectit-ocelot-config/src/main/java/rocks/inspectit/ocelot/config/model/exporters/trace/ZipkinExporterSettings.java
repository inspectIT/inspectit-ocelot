package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;
import rocks.inspectit.ocelot.config.utils.EndpointUtils;

@Data
@NoArgsConstructor
public class ZipkinExporterSettings {

    /**
     * Whether the exporter should be started.
     */
    private ExporterEnabledState enabled;

    @Deprecated
    /**
     * This property is deprecated since v2.0. Please use {@link #endpoint} instead.
     * The URL of the Zipkin server.
     */ private String url;

    /**
     * The URL endpoint of the Zipkin server.
     */
    private String endpoint;

    public String getUrl() {
        return EndpointUtils.padEndpoint(url);
    }

    /**
     * Gets the endpoint of the Zipkin server. The endpoint is padded with 'http' to meet OTEL's requirement that the URI needs to start with 'http://' or 'https://'.
     * E.g., if you set the endpoint to 'localhost:4317', it will be returned as 'http://localhost:4317'.
     *
     * @return
     */
    public String getEndpoint() {
        return EndpointUtils.padEndpoint(endpoint);
    }
}

package rocks.inspectit.ocelot.config.model.exporters.tags;

import lombok.Data;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import java.util.List;

/**
 * Settings for the HTTP-server tags exporter.
 */
@Data
public class HttpExporterSettings {

    /**
     * Whether the exporter should be started.
     */
    private ExporterEnabledState enabled;

    /**
     * The host of the HTTP-server
     */
    private String host;

    /**
     * The port of the HTTP-server
     */
    private int port;

    /**
     * The path for the endpoint of the HTTP-server
     */
    private String path;

    /**
     * How many threads at most can process requests
     */
    private int threadLimit;

    /**
     * List of allowed Origins, which are able to access the HTTP-server
     */
    private List<String> allowedOrigins;
}

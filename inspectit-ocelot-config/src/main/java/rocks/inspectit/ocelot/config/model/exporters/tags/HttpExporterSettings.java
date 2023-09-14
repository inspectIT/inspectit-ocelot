package rocks.inspectit.ocelot.config.model.exporters.tags;

import lombok.Data;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

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
     * How many sessions can be stored at the same time
     */
    private int sessionLimit;

    /**
     * How long the data should be stored in the server
     */
    private int timeToLive;

    /**
     * Key, which should be read during browser-propagation to receive the session-ID
     */
    private String sessionIdKey;
}

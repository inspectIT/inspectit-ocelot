package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

@Data
@NoArgsConstructor
public class ZipkinExporterSettings {

    /**
     * Whether the exporter should be started.
     */
    private ExporterEnabledState enabled;

    /**
     * The URL of the Zipkin server.
     */
    private String url;

}

package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

@Data
@NoArgsConstructor
public class ZipkinExporterSettings {

    private ExporterEnabledState enabled;

    /**
     * The URL of the Zipkin server.
     */
    private String url;

    /**
     * The service name under which traces are published, defaults to inspectit.service-name;
     */
    private String serviceName;

}

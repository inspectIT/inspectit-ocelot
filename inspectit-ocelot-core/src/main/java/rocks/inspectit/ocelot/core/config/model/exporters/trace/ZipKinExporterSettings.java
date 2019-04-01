package rocks.inspectit.ocelot.core.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ZipKinExporterSettings {

    private boolean enabled;

    /**
     * The URL of the ZipKin server.
     */
    private String url;

    /**
     * The service name under which traces are published, defaults to inspectit.service-name;
     */
    private String serviceName;

}

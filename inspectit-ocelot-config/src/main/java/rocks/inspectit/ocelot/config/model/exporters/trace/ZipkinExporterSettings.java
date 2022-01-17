package rocks.inspectit.ocelot.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ZipkinExporterSettings {

    private boolean enabled;

    /**
     * The URL of the Zipkin server.
     */
    private String url;

}

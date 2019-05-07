package rocks.inspectit.ocelot.core.config.model.exporters.trace;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Duration;

@Data
@NoArgsConstructor
public class OpenCensusAgentTraceExporterSettings {

    private boolean enabled;

    /**
     * The address of the OpenCensus Agent.
     */
    private String address;

    /**
     * Disable SSL
     */
    private boolean useInsecure;

    /**
     * The service name under which traces are published, defaults to inspectit.service-name:
     */
    private String serviceName;

    /**
     * Defines the reconnection period in seconds
     */
    @NotNull
    private Duration reconnectionPeriod;
}
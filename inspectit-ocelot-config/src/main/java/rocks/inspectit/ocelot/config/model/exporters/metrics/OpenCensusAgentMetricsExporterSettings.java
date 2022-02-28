package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledSetting;

import javax.validation.constraints.NotNull;
import java.time.Duration;

@Data
@NoArgsConstructor
public class OpenCensusAgentMetricsExporterSettings {

    private ExporterEnabledSetting enabled;

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

    /**
     * Defines the export interval in seconds
     */
    @NotNull
    private Duration exportInterval;
}
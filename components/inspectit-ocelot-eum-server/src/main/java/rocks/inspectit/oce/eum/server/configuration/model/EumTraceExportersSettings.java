package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;
import rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class EumTraceExportersSettings extends TraceExportersSettings {

    /**
     * Specifies whether client IP addresses which are added to spans should be masked.
     */
    private boolean maskSpanIpAddresses;
}

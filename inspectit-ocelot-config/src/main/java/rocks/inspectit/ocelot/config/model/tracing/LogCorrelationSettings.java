package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class LogCorrelationSettings {

    /**
     * Settings for the injection of trace ids into logging MDCs.
     */
    @Valid
    private TraceIdMDCInjectionSettings traceIdMdcInjection = new TraceIdMDCInjectionSettings();

    /**
     * Settings for the auto injection of trace ids into log messages.
     */
    @Valid
    private TraceIdAutoInjectionSettings traceIdAutoInjection = new TraceIdAutoInjectionSettings();
}

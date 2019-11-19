package rocks.inspectit.ocelot.config.model.instrumentation.experimental;

import lombok.Data;

import javax.validation.Valid;

/**
 * Settings of experimental features.
 */
@Data
public class ExperimentalSettings {

    /**
     * Settings for the auto injection of trace ids into log messages.
     */
    @Valid
    private TraceIdAutoInjectionSettings traceIdAutoInjectionSettings = new TraceIdAutoInjectionSettings();

}

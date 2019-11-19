package rocks.inspectit.ocelot.config.model.instrumentation.experimental;

import lombok.Data;

import javax.validation.Valid;

@Data
public class ExperimentalSettings {

    @Valid
    private TraceIdAutoInjectionSettings traceIdAutoInjectionSettings = new TraceIdAutoInjectionSettings();

}

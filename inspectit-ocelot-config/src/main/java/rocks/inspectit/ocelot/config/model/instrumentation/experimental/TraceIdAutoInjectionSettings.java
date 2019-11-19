package rocks.inspectit.ocelot.config.model.instrumentation.experimental;

import lombok.Data;

@Data
public class TraceIdAutoInjectionSettings {

    private boolean enabled;

    private String prefix;

    private String suffix;
}

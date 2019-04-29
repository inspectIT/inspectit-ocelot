package rocks.inspectit.ocelot.core.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TracingSettings {

    /**
     * Master switch for disabling trace recording and exporting.
     * If disabled the following happens:
     * - all trace exporters are disabled
     * - tracing will be disabled for all instrumentation rules
     */
    private boolean enabled;

}

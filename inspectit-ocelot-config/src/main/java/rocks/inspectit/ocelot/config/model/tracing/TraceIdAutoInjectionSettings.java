package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for the auto injection of trace ids into log messages. The trace id will be injected into a message in the
 * following format: [PREFIX]_trace_id_[SUFFIX]_message_
 */
@Data
@NoArgsConstructor
public class TraceIdAutoInjectionSettings {

    /**
     * Whether the auto injection is enabled.
     */
    private boolean enabled;

    /**
     * The used prefix.
     */
    private String prefix;

    /**
     * The used suffix.
     */
    private String suffix;
}

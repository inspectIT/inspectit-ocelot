package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class TraceIdMDCInjectionSettings {

    /**
     * Specifies whether log correlation shall be performed or not.
     * If enabled, the currently active traceID will be published to the MDC of all log libraries.
     */
    private boolean enabled;

    /**
     * The key under which the traceid is placed in the MDCs.
     */
    @NotBlank
    private String key;
}

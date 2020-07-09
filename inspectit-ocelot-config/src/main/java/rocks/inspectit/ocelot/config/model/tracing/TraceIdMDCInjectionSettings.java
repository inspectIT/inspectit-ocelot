package rocks.inspectit.ocelot.config.model.tracing;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class TraceIdMDCInjectionSettings {

    /**
     * Specifies whether log correlation shall be performed or not.
     * If enabled, the currently active traceID will be published to the MDC of all log libraries,
     * if they have not been explicitly disabled.
     */
    private boolean enabled;

    /**
     * The key under which the traceid is placed in the MDCs.
     */
    @NotBlank
    private String key;

    /**
     * Log4J2 injection will only take place, if this field and {@link #enabled} are true.
     */
    private boolean log4j2Enabled;

    /**
     * Log4J1 injection will only take place, if this field and {@link #enabled} are true.
     */
    private boolean log4j1Enabled;

    /**
     * Slf4j injection will only take place, if this field and {@link #enabled} are true.
     */
    private boolean slf4jEnabled;

    /**
     * JBoss LogManager injection will only take place, if this field and {@link #enabled} are true.
     */
    private boolean jbossLogmanagerEnabled;
}

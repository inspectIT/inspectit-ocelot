package rocks.inspectit.ocelot.config.model.instrumentation.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

/**
 * At the moment the settings only allow to record synchronous invocations.
 * <p>
 * To record asynchronous invocations, we could use a similar approach as tracing does.
 * For instance additional properties to start & end an invocation within a rule,
 * as well as some way to link an invocation over multiple rules.
 */
@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
public class ConcurrentInvocationSettings {

    public static final ConcurrentInvocationSettings NO_RECORDING = ConcurrentInvocationSettings.builder()
            .enabled(false)
            .build();

    /**
     * If true, concurrent invocations of methods within the rule will be recorded.
     * Additionally, the {@link MetricsSettings#concurrent} has to be enabled, otherwise we will not export the recorded values.
     */
    private Boolean enabled;

    /**
     * The name of the operation, whose invocation will be recorded.
     * The name will be used as tag for the OpenCensus measurement so we can filter for specific operations.
     */
    private String operation;
}

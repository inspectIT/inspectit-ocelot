package rocks.inspectit.ocelot.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for the {@link SpecialSensor}s.
 */
@Data
@NoArgsConstructor
public class SpecialSensorSettings {

    /**
     * Enables or disables the {@link rocks.inspectit.ocelot.core.instrumentation.special.ExecutorContextPropagationSensor}.
     */
    private boolean executorContextPropagation;

    /**
     * Enables or disables the {@link rocks.inspectit.ocelot.core.instrumentation.special.ThreadStartContextPropagationSensor}.
     */
    private boolean threadStartContextPropagation;

    /**
     * Enables or disables the @{@link rocks.inspectit.ocelot.core.instrumentation.special.ScheduledExecutorContextPropagationSensor}.
     */
    private boolean scheduledExecutorContextPropagation;

    /**
     * If true, we instrument all class loaders which contain instrumented classes to make sure our bootstrap classes are reachable.
     * This ensures that in custom module systems such as OSGi our instrumentation works without the need for configuration changes.
     */
    private boolean classLoaderDelegation;

}

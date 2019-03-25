package rocks.inspectit.ocelot.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;
import rocks.inspectit.ocelot.core.instrumentation.special.ExecutorContextPropagationSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.ScheduledExecutorContextPropagationSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.ThreadStartContextPropagationSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.remote.ApacheHttpClientContextPropagationSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.remote.HttpUrlConnectionContextPropagationSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.remote.ServletApiContextDownPropagationSensor;
import rocks.inspectit.ocelot.core.instrumentation.special.remote.ServletApiContextUpPropagationSensor;

/**
 * Settings for the {@link SpecialSensor}s.
 */
@Data
@NoArgsConstructor
public class SpecialSensorSettings {

    /**
     * Enables or disables the {@link ExecutorContextPropagationSensor}.
     */
    private boolean executorContextPropagation;

    /**
     * Enables or disables the {@link ThreadStartContextPropagationSensor}.
     */
    private boolean threadStartContextPropagation;

    /**
     * Enables or disables the @{@link ScheduledExecutorContextPropagationSensor}.
     */
    private boolean scheduledExecutorContextPropagation;

    /**
     * If true, we instrument all class loaders which contain instrumented classes to make sure our bootstrap classes are reachable.
     * This ensures that in custom module systems such as OSGi our instrumentation works without the need for configuration changes.
     */
    private boolean classLoaderDelegation;

    /**
     * Enables or disables {@link HttpUrlConnectionContextPropagationSensor}.
     */
    private boolean httpUrlConnectionContextPropagation;

    /**
     * Enables or disable {@link ApacheHttpClientContextPropagationSensor}
     */
    private boolean apacheHttpClientContextPropagation;

    /**
     * Enables or disables {@link ServletApiContextUpPropagationSensor} and
     * {@link ServletApiContextDownPropagationSensor}
     */
    private boolean servletApiContextPropagation;
}

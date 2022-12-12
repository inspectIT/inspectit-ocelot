package rocks.inspectit.ocelot.core.instrumentation.special;

import net.bytebuddy.dynamic.DynamicType;
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;

public interface SpecialSensor {

    /**
     * Evaluates for the given type if this sensor requires a instrumentation given a configuration.
     * If this method returns true, {@link #instrument(TypeDescriptionWithClassLoader, InstrumentationConfiguration, DynamicType.Builder)}
     * will be called later to perform the instrumentation.
     *
     * @param typeWithLoader the type to check for
     * @param settings       the configuration ot check for
     *
     * @return true if this sensor is active and requires an instrumentation for the given type under the given configuration
     */
    boolean shouldInstrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings);

    /**
     * Evaluates whether the instrumentation has to change given a certain configuration change.
     * This is invoked to decide if it is necessary to re-instrument a class given a configuration change.
     * <p>
     * It is guaranteed that this method is only invoked if {@link #shouldInstrument(TypeDescriptionWithClassLoader, InstrumentationConfiguration)}
     * return true for both given configurations for the given type, therefore this does not need to be checked.
     *
     * @param typeWithLoader the type to check for
     * @param first          the first configuration
     * @param second         the second configuration
     *
     * @return true, if a configuration change from "first" to "second" (or vice-versa) would require a change in the bytecode added by this sensor.
     */
    boolean requiresInstrumentationChange(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration first, InstrumentationConfiguration second);

    /**
     * Apply the given instrumentation on the given type.
     * This method is guaranteed to be only invoked when {@link #shouldInstrument(TypeDescriptionWithClassLoader, InstrumentationConfiguration)} for the same parameters returns true.
     *
     * @param typeWithLoader the class being instrumented
     * @param settings       the configuration to apply
     * @param builder        the builder to use for defining the instrumentation
     *
     * @return the altered builder
     */
    DynamicType.Builder instrument(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration settings, DynamicType.Builder builder);

}

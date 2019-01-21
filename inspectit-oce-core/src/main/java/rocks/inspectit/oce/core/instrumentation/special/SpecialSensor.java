package rocks.inspectit.oce.core.instrumentation.special;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import rocks.inspectit.oce.core.instrumentation.config.InstrumentationConfiguration;

public interface SpecialSensor {

    /**
     * Evaluates for the given type if this sensor requires a instrumentation given a configuration.
     * If this method returns true, {@link #instrument(TypeDescription, InstrumentationConfiguration, DynamicType.Builder)}
     * will be called later to perform the instrumentation.
     *
     * @param type     the type to check for
     * @param settings the configuration ot check for
     * @return true if this sensor is active and requires an instrumentation for the given type under the given configuration
     */
    boolean shouldInstrument(TypeDescription type, InstrumentationConfiguration settings);

    /**
     * Evaluates whether the instrumentation has to change given a certain configuration change.
     * This is invoked to decide if it is necessary to re-instrument a class given a configuration change.
     * <p>
     * It is guaranteed that this method is only invoked if {@link #shouldInstrument(TypeDescription, InstrumentationConfiguration)}
     * return true for both given configurations for the given type, therefore this does not need to be checked.
     *
     * @param type   the type to check for
     * @param first  the first configuration
     * @param second the second configuration
     * @return true, if a configuration change from "first" to "second" (or vice-versa) would require a change in the bytecode added by this sensor.
     */
    boolean requiresInstrumentationChange(TypeDescription type, InstrumentationConfiguration first, InstrumentationConfiguration second);

    /**
     * Apply the given instrumentation on the given type.
     * This method is guaranteed to be only invoked when {@link #shouldInstrument(TypeDescription, InstrumentationConfiguration)} for the same parameters returns true.
     *
     * @param clazz    the class being instrumented
     * @param type     the type descriptor representing the class
     * @param settings the configuration to apply
     * @param builder  the builder to use for defining the instrumentation
     * @return the altered builder
     */
    DynamicType.Builder instrument(Class<?> clazz, TypeDescription type, InstrumentationConfiguration settings, DynamicType.Builder builder);

}

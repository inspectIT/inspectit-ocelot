package rocks.inspectit.oce.core.instrumentation;

import lombok.Value;
import net.bytebuddy.description.type.TypeDescription;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the actual or requested state of the instrumentation of a single class.
 */
@Value
class ClassInstrumentationConfiguration {

    /**
     * The configuration representing that no instrumentation of the class is performed.
     */
    static final ClassInstrumentationConfiguration NO_INSTRUMENTATION = new ClassInstrumentationConfiguration(Collections.emptySet(), null);

    /**
     * The sensors which are active for the target class.
     */
    Set<SpecialSensor> activeSensors;

    /**
     * The inspectIT configuration used to derive this configuration object.
     * Can be null, but only if no sensors active.
     */
    InstrumentationSettings activeConfiguration;

    /**
     * Compares this instrumentation configuration against another.
     * Two instrumentations are considered to be the "same" if they result in the same bytecode changes.
     *
     * @param forType the type for which this configuration is meant.
     * @param other   the other configuration to compare against
     * @return true if both are the same, false otherwise
     */
    public boolean isSameAs(TypeDescription forType, ClassInstrumentationConfiguration other) {
        if (!activeSensors.equals(other.activeSensors)) {
            return false;
        }
        for (SpecialSensor sensor : activeSensors) {
            if (sensor.requiresInstrumentationChange(forType, activeConfiguration, other.activeConfiguration)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the instrumentation configuration for a class based on the overall {@link rocks.inspectit.oce.core.config.model.InspectitConfig}.
     *
     * @param forType    the type for which the configuration shall be derived
     * @param config     the active inspectIT config
     * @param allSensors the list of all available sensors
     * @return
     */
    public static ClassInstrumentationConfiguration getFor(TypeDescription forType, InstrumentationSettings config, Collection<? extends SpecialSensor> allSensors) {
        Set<SpecialSensor> activeSensors = allSensors.stream()
                .filter(s -> s.shouldInstrument(forType, config))
                .collect(Collectors.toSet());
        return new ClassInstrumentationConfiguration(activeSensors, config);
    }

}

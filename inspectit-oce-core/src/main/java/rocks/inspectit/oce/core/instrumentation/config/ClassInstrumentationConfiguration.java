package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Getter;
import net.bytebuddy.description.type.TypeDescription;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import java.util.Collections;
import java.util.Set;

/**
 * Defines the actual or requested state of the instrumentation of a single class.
 */
public class ClassInstrumentationConfiguration {

    /**
     * The configuration representing that no instrumentation of the class is performed.
     */
    public static final ClassInstrumentationConfiguration NO_INSTRUMENTATION = new ClassInstrumentationConfiguration(Collections.emptySet(), null);

    /**
     * The sensors which are active for the target class.
     */
    @Getter
    private final Set<SpecialSensor> activeSpecialSensors;

    /**
     * The inspectIT configuration used to derive this configuration object.
     * Can be null, but only if no sensors active.
     */
    @Getter
    private final InstrumentationConfiguration activeConfiguration;

    public ClassInstrumentationConfiguration(Set<SpecialSensor> activeSpecialSensors, InstrumentationConfiguration activeConfiguration) {
        this.activeSpecialSensors = activeSpecialSensors;
        this.activeConfiguration = activeConfiguration;
    }

    /**
     * Compares this instrumentation configuration against another.
     * Two instrumentations are considered to be the "same" if they result in the same bytecode changes.
     *
     * @param forType the type for which this configuration is meant.
     * @param other   the other configuration to compare against
     * @return true if both are the same, false otherwise
     */
    public boolean isSameAs(TypeDescription forType, ClassInstrumentationConfiguration other) {
        if (!activeSpecialSensors.equals(other.activeSpecialSensors)) {
            return false;
        }
        for (SpecialSensor sensor : activeSpecialSensors) {
            if (sensor.requiresInstrumentationChange(forType, activeConfiguration, other.activeConfiguration)) {
                return false;
            }
        }
        return true;
    }

}

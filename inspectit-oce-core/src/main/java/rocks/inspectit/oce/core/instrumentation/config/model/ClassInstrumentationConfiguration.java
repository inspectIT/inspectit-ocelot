package rocks.inspectit.oce.core.instrumentation.config.model;

import lombok.Getter;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.oce.core.utils.CommonUtils;

import java.util.Collections;
import java.util.Set;

/**
 * Defines the actual or requested state of the instrumentation of a single class.
 */
public class ClassInstrumentationConfiguration {

    /**
     * The configuration representing that no instrumentation of the class is performed.
     */
    public static final ClassInstrumentationConfiguration NO_INSTRUMENTATION = new ClassInstrumentationConfiguration(Collections.emptySet(), Collections.emptySet(), null);

    /**
     * The sensors which are active for the target class.
     */
    @Getter
    private final Set<SpecialSensor> activeSpecialSensors;

    @Getter
    private Set<InstrumentationRule> activeRules;

    /**
     * The inspectIT configuration used to derive this configuration object.
     * Can be null, but only if no sensors active.
     */
    @Getter
    private final InstrumentationConfiguration activeConfiguration;

    public ClassInstrumentationConfiguration(Set<SpecialSensor> activeSpecialSensors, Set<InstrumentationRule> activeRules, InstrumentationConfiguration activeConfiguration) {
        this.activeSpecialSensors = activeSpecialSensors;
        this.activeRules = activeRules;
        this.activeConfiguration = activeConfiguration;
    }

    /**
     * Compares this instrumentation configuration against another.
     * Two instrumentations are considered to be the "same" if they result in the same bytecode changes.
     * To check if a given configuration represents "no instrumentation" you should rather use {@link #isNoInstrumentation()}
     * isntead of comparing against {@link #NO_INSTRUMENTATION}.
     *
     * @param clazz the type for which this configuration is meant.
     * @param other the other configuration to compare against
     * @return true if both are the same, false otherwise
     */
    public boolean isSameAs(Class<?> clazz, ClassInstrumentationConfiguration other) {
        if (!CommonUtils.contentsEqual(activeSpecialSensors, other.activeSpecialSensors)) {
            return false;
        }
        if (!CommonUtils.contentsEqual(getActiveRules(), other.getActiveRules())) {
            return false;
        }
        for (SpecialSensor sensor : activeSpecialSensors) {
            if (sensor.requiresInstrumentationChange(clazz, activeConfiguration, other.activeConfiguration)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this configuration induces no bytecode changes to the target class.
     * This is the same as invoking {@link #isSameAs(Class, ClassInstrumentationConfiguration)}
     * with {@link #NO_INSTRUMENTATION}, but faster.
     *
     * @return true iof this configuration represents "no instrumentation"
     */
    public boolean isNoInstrumentation() {
        return CollectionUtils.isEmpty(activeSpecialSensors) && CollectionUtils.isEmpty(activeRules);
    }

}

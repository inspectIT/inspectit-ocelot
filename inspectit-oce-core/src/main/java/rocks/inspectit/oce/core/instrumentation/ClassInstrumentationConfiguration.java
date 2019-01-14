package rocks.inspectit.oce.core.instrumentation;

import lombok.Value;
import net.bytebuddy.description.type.TypeDescription;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Value
class ClassInstrumentationConfiguration {

    static final ClassInstrumentationConfiguration NO_INSTRUMENTATION = new ClassInstrumentationConfiguration(Collections.EMPTY_SET, null);

    Set<SpecialSensor> activeSensors;
    //can be null
    InstrumentationSettings activeConfiguration;

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


    public static ClassInstrumentationConfiguration getFor(TypeDescription forType, InstrumentationSettings config, Collection<SpecialSensor> allSensors) {
        Set<SpecialSensor> activeSensors = allSensors.stream()
                .filter(s -> s.shouldInstrument(forType, config))
                .collect(Collectors.toSet());
        return new ClassInstrumentationConfiguration(activeSensors, config);
    }

}

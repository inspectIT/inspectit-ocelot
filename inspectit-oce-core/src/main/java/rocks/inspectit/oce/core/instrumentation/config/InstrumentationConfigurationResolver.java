package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Getter;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.AsyncClassTransformer;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for deriving the {@link InstrumentationConfiguration} from teh {@link InstrumentationSettings}.
 */
@Service
public class InstrumentationConfigurationResolver {

    private static final ClassLoader INSPECTIT_CLASSLOADER = AsyncClassTransformer.class.getClassLoader();

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private Instrumentation instrumentation;

    @Autowired
    private List<SpecialSensor> specialSensors;

    /**
     * Holds the currently active instrumentation configuration.
     */
    @Getter
    private InstrumentationConfiguration currentConfig;

    @PostConstruct
    private void init() {
        updateConfiguration(env.getCurrentConfig().getInstrumentation());
    }

    /**
     * Builds the {@link ClassInstrumentationConfiguration} based on the currently active global instrumentation configuration
     * for the given class.
     *
     * @param clazz the class for which the configuration shal lbe queried
     * @return the configuration or {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION} if this class should not be instrumented
     */
    public ClassInstrumentationConfiguration getClassInstrumentationConfiguration(Class<?> clazz) {
        val config = currentConfig;
        if (isIgnoredClass(clazz, config)) {
            return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;

        } else {
            TypeDescription description = TypeDescription.ForLoadedType.of(clazz);
            Set<SpecialSensor> activeSensors = specialSensors.stream()
                    .filter(s -> s.shouldInstrument(description, config))
                    .collect(Collectors.toSet());
            return new ClassInstrumentationConfiguration(activeSensors, config);
        }
    }

    @EventListener
    private void inspectitConfigChanged(InspectitConfigChangedEvent ev) {
        if (!Objects.equals(ev.getNewConfig().getInstrumentation(), currentConfig.getSource())) {
            val oldConfig = currentConfig;
            updateConfiguration(ev.getNewConfig().getInstrumentation());

            val event = new InstrumentationConfigurationChangedEvent(this, oldConfig, currentConfig);
            ctx.publishEvent(event);
        }
    }

    private void updateConfiguration(InstrumentationSettings source) {
        //Not much to do yet here
        //in the future we can for example process the active profiles here
        currentConfig = new InstrumentationConfiguration(source);
    }

    /**
     * Checks if the given class should not be instrumented based on the given configuration.
     *
     * @param clazz  the class to check
     * @param config configuration to check for
     * @return true, if the class is ignored (=it should not be instrumented)
     */
    private boolean isIgnoredClass(Class<?> clazz, InstrumentationConfiguration config) {
        if (!instrumentation.isModifiableClass(clazz)) {
            return true;
        }

        if (clazz.getClassLoader() == INSPECTIT_CLASSLOADER) {
            return true;
        }
        if (clazz.getClassLoader() == null) {
            String name = clazz.getName();
            boolean isIgnored = env.getCurrentConfig().getInstrumentation().getIgnoredBootstrapPackages().entrySet().stream()
                    .filter(e -> e.getValue() == true)
                    .anyMatch(e -> name.startsWith(e.getKey() + "."));
            if (isIgnored) {
                return true;
            }
        }
        return false;
    }
}

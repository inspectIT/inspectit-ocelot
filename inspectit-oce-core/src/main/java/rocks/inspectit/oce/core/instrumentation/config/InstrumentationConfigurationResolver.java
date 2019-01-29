package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Getter;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.oce.core.instrumentation.AsyncClassTransformer;
import rocks.inspectit.oce.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.oce.core.instrumentation.config.model.ClassInstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.special.SpecialSensor;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is responsible for deriving the {@link InstrumentationConfiguration} from
 * the {@link InstrumentationSettings}.
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

    @Autowired
    private InstrumentationRuleResolver ruleResolver;

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

//            Set<InstrumentationRule> activeRules = new HashSet<>();
//            for (InstrumentationRule rule : currentConfig.getRules()) {
////                for (InstrumentationScope scope : rule.getScopes()) {
////                    if (scope.getTypeMatcher().matches(description) ) {
////
////                    }
////                }
//                Set<InstrumentationScope> matchingScopes = rule.getScopes().stream()
//                        .filter(s -> s.getTypeMatcher().matches(description))
//                        .collect(Collectors.toSet());
//
////                Set<InstrumentationScope> matchingScopes = rule.getScopes().stream()
////                        .filter(s -> s.getTypeMatcher().matches(description))
////                        .collect(Collectors.toSet());
//
//                if (!matchingScopes.isEmpty()) {
//                    activeRules.add(new InstrumentationRule(rule.getName(), matchingScopes));
//                }
//            }

            Set<InstrumentationRule> activeRules = currentConfig.getRules().stream()
                    .map(rule -> Pair.of(
                            rule.getName(),
                            rule.getScopes()
                                    .stream()
                                    .filter(s -> s.getTypeMatcher().matches(description))
                                    .collect(Collectors.toSet())))
                    .filter(p -> !p.getRight().isEmpty())
                    .map(p -> new InstrumentationRule(p.getLeft(), p.getRight()))
                    .collect(Collectors.toSet());


//            Set<InstrumentationRule> activeRules = currentConfig.getRules().stream()
//                    .filter(r -> r.getScopes()
//                            .stream()
//                            .anyMatch(s -> s.getTypeMatcher().matches(description)))
//                    .collect(Collectors.toSet());

            return new ClassInstrumentationConfiguration(activeSensors, activeRules, config);
        }
    }

    @EventListener
    private void inspectitConfigChanged(InspectitConfigChangedEvent ev) {

        if (haveInstrumentationRelatedSettingsChanged(ev)) {
            val oldConfig = currentConfig;
            updateConfiguration(ev.getNewConfig().getInstrumentation());

            val event = new InstrumentationConfigurationChangedEvent(this, oldConfig, currentConfig);
            ctx.publishEvent(event);
        }
    }

    private boolean haveInstrumentationRelatedSettingsChanged(InspectitConfigChangedEvent ev) {
        InstrumentationSettings oldC = ev.getOldConfig().getInstrumentation();
        InstrumentationSettings newC = ev.getNewConfig().getInstrumentation();

        if (!Objects.equals(oldC.getIgnoredBootstrapPackages(), newC.getIgnoredBootstrapPackages())) {
            return true;
        }
        if (!Objects.equals(oldC.getSpecial(), newC.getSpecial())) {
            return true;
        }
        if (!Objects.equals(oldC.getRules(), newC.getRules())) {
            return true;
        }
        if (!Objects.equals(oldC.getScopes(), newC.getScopes())) {
            return true;
        }
        return false;
    }

    private void updateConfiguration(InstrumentationSettings source) {
        //Not much to do yet here
        //in the future we can for example process the active profiles here
        Set<InstrumentationRule> rules = ruleResolver.resolve(source);

        currentConfig = new InstrumentationConfiguration(source, rules);
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
                    .filter(Map.Entry::getValue)
                    .anyMatch(e -> name.startsWith(e.getKey()));
            if (isIgnored) {
                return true;
            }
        }
        return false;
    }
}

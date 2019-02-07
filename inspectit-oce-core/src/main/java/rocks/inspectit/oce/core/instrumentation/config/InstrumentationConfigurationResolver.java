package rocks.inspectit.oce.core.instrumentation.config;

import lombok.Getter;
import lombok.val;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.bootstrap.instrumentation.DoNotInstrumentMarker;
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

    @Autowired
    private DataProviderResolver dataProviderResolver;

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

            Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(description, config);

            return new ClassInstrumentationConfiguration(activeSensors, narrowedRules, config);
        }
    }

    /**
     * Narrows a rule for a specific type. The rules existing in the returned set are containing only {@link InstrumentationScope}s
     * which are matching for the given type. This prevents that method matchers will be applied to the wrong types.
     *
     * @param typeDescription the class which are the rules targeting
     * @param config          the configuration which is used as basis for the rules
     * @return Returns a set containing rules with scopes targeting only the given type.
     */
    private Set<InstrumentationRule> getNarrowedRulesFor(TypeDescription typeDescription, InstrumentationConfiguration config) {
        return config.getRules().stream()
                .map(rule -> Pair.of(
                        rule.getName(),
                        rule.getScopes()
                                .stream()
                                .filter(s -> s.getTypeMatcher().matches(typeDescription))
                                .collect(Collectors.toSet())))
                .filter(p -> !p.getRight().isEmpty())
                .map(p -> new InstrumentationRule(p.getLeft(), p.getRight()))
                .collect(Collectors.toSet());
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
        InstrumentationSettings oldConfig = ev.getOldConfig().getInstrumentation();
        InstrumentationSettings newConfig = ev.getNewConfig().getInstrumentation();

        if (!Objects.equals(oldConfig.getIgnoredBootstrapPackages(), newConfig.getIgnoredBootstrapPackages())) {
            return true;
        }
        if (!Objects.equals(oldConfig.getSpecial(), newConfig.getSpecial())) {
            return true;
        }
        if (!Objects.equals(oldConfig.getRules(), newConfig.getRules())) {
            return true;
        }
        if (!Objects.equals(oldConfig.getScopes(), newConfig.getScopes())) {
            return true;
        }
        if (!Objects.equals(oldConfig.getDataProviders(), newConfig.getDataProviders())) {
            return true;
        }
        return false;
    }

    private void updateConfiguration(InstrumentationSettings source) {
        currentConfig = InstrumentationConfiguration.builder()
                .source(source)
                .rules(ruleResolver.resolve(source))
                .dataProviders(dataProviderResolver.resolveProviders(source))
                .build();
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

        if (DoNotInstrumentMarker.class.isAssignableFrom(clazz)) {
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

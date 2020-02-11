package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.instrumentation.InstrumentationSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.instrumentation.AsyncClassTransformer;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.instrumentation.config.model.*;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import javax.annotation.PostConstruct;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is responsible for deriving the {@link InstrumentationConfiguration} from
 * the {@link InstrumentationSettings}.
 */
@Service
@Slf4j
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
    private GenericActionConfigurationResolver genericActionConfigurationResolver;

    @Autowired
    private MethodHookConfigurationResolver hookResolver;

    @Autowired
    private PropagationMetaDataResolver propagationMetaDataResolver;


    /**
     * Holds the currently active instrumentation configuration.
     */
    @Getter
    private InstrumentationConfiguration currentConfig;

    @PostConstruct
    private void init() {
        InspectitConfig conf = env.getCurrentConfig();
        currentConfig = resolveConfiguration(conf);
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
        try {
            if (isIgnoredClass(clazz, config)) {
                return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;

            } else {
                TypeDescription description = TypeDescription.ForLoadedType.of(clazz);
                Set<SpecialSensor> activeSensors = specialSensors.stream()
                        .filter(s -> s.shouldInstrument(clazz, config))
                        .collect(Collectors.toSet());

                Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(description, config);

                return new ClassInstrumentationConfiguration(activeSensors, narrowedRules, config);

            }
        } catch (NoClassDefFoundError e) {
            //the class contains a reference to an not loadable class
            //this the case for example for very many spring boot classes
            log.trace("Ignoring class {} for instrumentation as it is not initializable ", clazz.getName(), e);
            return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
        }
    }

    /**
     * Finds out for each method of the given class which rules apply and builds a {@link MethodHookConfiguration} for each instrumented method.
     *
     * @param clazz the class to check
     * @return a map mapping hook configurations to the methods which they should be applied on.
     */
    public Map<MethodDescription, MethodHookConfiguration> getHookConfigurations(Class<?> clazz) {
        val config = currentConfig;
        if (isIgnoredClass(clazz, config)) {
            return Collections.emptyMap();
        }
        try {
            TypeDescription type = TypeDescription.ForLoadedType.of(clazz);
            Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(type, config);

            Set<InstrumentationScope> involvedScopes = narrowedRules.stream()
                    .flatMap(r -> r.getScopes().stream())
                    .collect(Collectors.toSet());

            if (!narrowedRules.isEmpty()) {
                Map<MethodDescription, MethodHookConfiguration> result = new HashMap<>();
                for (val method : type.getDeclaredMethods()) {
                    val rulesMatchingOnMethod = narrowedRules.stream()
                            .filter(rule -> rule.getScopes().stream()
                                    .anyMatch(scope -> scope.getMethodMatcher().matches(method)))
                            .collect(Collectors.toSet());
                    if (!rulesMatchingOnMethod.isEmpty()) {
                        try {
                            result.put(method, hookResolver.buildHookConfiguration(config, rulesMatchingOnMethod));
                        } catch (Exception e) {
                            log.error("Could not build hook for {} of class {}",
                                    CoreUtils.getSignature(method), clazz.getName(), e);
                        }
                    }
                }
                return result;
            }
        } catch (NoClassDefFoundError e) {
            //the class contains a reference to an not loadable class
            //this the case for example for very many spring boot classes
            log.trace("Ignoring class {} for hooking as it is not initializable ", clazz.getName(), e);
        }
        return Collections.emptyMap();


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
                        rule,
                        rule.getScopes()
                                .stream()
                                .filter(s -> s.getTypeMatcher().matches(typeDescription))
                                .collect(Collectors.toSet())))
                .filter(p -> !p.getRight().isEmpty())
                .map(p -> p.getLeft().toBuilder().clearScopes().scopes(p.getRight()).build())
                .collect(Collectors.toSet());
    }

    @EventListener
    private void inspectitConfigChanged(InspectitConfigChangedEvent ev) {

        InstrumentationConfiguration oldConfig = currentConfig;
        InstrumentationConfiguration newConfig = resolveConfiguration(ev.getNewConfig());
        if (!Objects.equals(oldConfig, newConfig)) {
            currentConfig = newConfig;
            val event = new InstrumentationConfigurationChangedEvent(this, oldConfig, currentConfig);
            ctx.publishEvent(event);
        }
    }

    private InstrumentationConfiguration resolveConfiguration(InspectitConfig config) {
        val genericActions = genericActionConfigurationResolver.resolveActions(config.getInstrumentation());
        return InstrumentationConfiguration.builder()
                .metricsEnabled(config.getMetrics().isEnabled())
                .tracingEnabled(config.getTracing().isEnabled())
                .tracingSettings(config.getTracing())
                .defaultTraceSampleProbability(config.getTracing().getSampleProbability())
                .source(config.getInstrumentation())
                .rules(ruleResolver.resolve(config.getInstrumentation(), genericActions))
                .propagationMetaData(propagationMetaDataResolver.resolve(config))
                .build();
    }

    /**
     * Checks if the given class should not be instrumented based on the given configuration.
     *
     * @param clazz  the class to check
     * @param config configuration to check for
     * @return true, if the class is ignored (=it should not be instrumented)
     */
    @VisibleForTesting
    boolean isIgnoredClass(Class<?> clazz, InstrumentationConfiguration config) {

        ClassLoader loader = clazz.getClassLoader();

        if (!instrumentation.isModifiableClass(clazz)) {
            return true;
        }

        if (DoNotInstrumentMarker.class.isAssignableFrom(clazz)) {
            return true;
        }

        if (loader != null && DoNotInstrumentMarker.class.isAssignableFrom(loader.getClass())) {
            return true;
        }

        if (loader == INSPECTIT_CLASSLOADER) {
            return true;
        }

        if (config.getSource().isExcludeLambdas() && clazz.getName().contains("$$Lambda$")) {
            return true;
        }

        String name = clazz.getName();

        boolean isIgnored = config.getSource().getIgnoredPackages().entrySet().stream()
                .filter(Map.Entry::getValue)
                .anyMatch(e -> name.startsWith(e.getKey()));
        if (isIgnored) {
            return true;
        }

        if (clazz.getClassLoader() == null) {
            boolean isIgnoredOnBootstrap = config.getSource().getIgnoredBootstrapPackages().entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .anyMatch(e -> name.startsWith(e.getKey()));
            if (isIgnoredOnBootstrap) {
                return true;
            }
        }
        return false;
    }
}

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
import rocks.inspectit.ocelot.core.instrumentation.TypeDescriptionWithClassLoader;
import rocks.inspectit.ocelot.core.instrumentation.config.event.InstrumentationConfigurationChangedEvent;
import rocks.inspectit.ocelot.core.instrumentation.config.model.*;
import rocks.inspectit.ocelot.core.instrumentation.context.session.PropagationSessionStorage;
import rocks.inspectit.ocelot.core.instrumentation.special.SpecialSensor;
import rocks.inspectit.ocelot.core.instrumentation.transformer.AsyncClassTransformer;
import rocks.inspectit.ocelot.core.utils.CoreUtils;

import javax.annotation.PostConstruct;
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
    private List<SpecialSensor> specialSensors;

    @Autowired
    private InstrumentationRuleResolver ruleResolver;

    @Autowired
    private GenericActionConfigurationResolver genericActionConfigurationResolver;

    @Autowired
    private MethodHookConfigurationResolver hookResolver;

    @Autowired
    private PropagationMetaDataResolver propagationMetaDataResolver;

    @Autowired
    private PropagationSessionStorage sessionStorage;

    /**
     * Holds the currently active instrumentation configuration.
     */
    @Getter
    private InstrumentationConfiguration currentConfig;

    @PostConstruct
    private void init() {
        InspectitConfig conf = env.getCurrentConfig();
        currentConfig = resolveConfiguration(conf);
        // set the initial propagation for the session storage
        sessionStorage.setPropagation(currentConfig.getPropagationMetaData());
    }

    /**
     * Builds the {@link ClassInstrumentationConfiguration} based on the currently active global instrumentation configuration
     * for the {@link TypeDescription} of the given {@link TypeDescriptionWithClassLoader}.
     *
     * @param typeWithLoader the {@link  TypeDescriptionWithClassLoader} for which the configuration shall lbe queried
     *
     * @return the configuration or {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION} if this class should not be instrumented
     */
    public ClassInstrumentationConfiguration getClassInstrumentationConfiguration(TypeDescriptionWithClassLoader typeWithLoader) {
        InstrumentationConfiguration config = currentConfig;
        try {
            if (!config.getSource().isEnabled() || isIgnoredClass(typeWithLoader, config)) {
                return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
            } else {
                Set<SpecialSensor> activeSensors = specialSensors.stream()
                        .filter(s -> s.shouldInstrument(typeWithLoader, config))
                        .collect(Collectors.toSet());

                Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(typeWithLoader.getType(), config);

                return new ClassInstrumentationConfiguration(activeSensors, narrowedRules, config);

            }
        } catch (NoClassDefFoundError e) {
            //the class contains a reference to a not loadable class
            //this the case for example for very many spring boot classes
            log.trace("Ignoring class {} for instrumentation as it is not initializable ", typeWithLoader.getName(), e);
            return ClassInstrumentationConfiguration.NO_INSTRUMENTATION;
        }
    }

    /**
     * Builds the {@link ClassInstrumentationConfiguration} based on the currently active global instrumentation configuration
     * for the given class.
     *
     * @param clazz the {@link  Class} for which the configuration shall lbe queried
     *
     * @return the configuration or {@link ClassInstrumentationConfiguration#NO_INSTRUMENTATION} if this class should not be instrumented
     */
    public ClassInstrumentationConfiguration getClassInstrumentationConfiguration(Class<?> clazz) {
        return getClassInstrumentationConfiguration(TypeDescriptionWithClassLoader.of(clazz));
    }

    /**
     * Finds out for each method of the given {@link TypeDescription} of the given {@link TypeDescriptionWithClassLoader}
     * which rules apply and builds a {@link MethodHookConfiguration} for each instrumented method.
     *
     * @param typeWithLoader the {@link  TypeDescriptionWithClassLoader} to check
     *
     * @return a map mapping hook configurations to the methods which they should be applied on.
     */
    public Map<MethodDescription, MethodHookConfiguration> getHookConfigurations(TypeDescriptionWithClassLoader typeWithLoader) {
        val config = currentConfig;
        if (isIgnoredClass(typeWithLoader, config)) {
            return Collections.emptyMap();
        }
        try {
            TypeDescription type = typeWithLoader.getType();
            Set<InstrumentationRule> narrowedRules = getNarrowedRulesFor(type, config);

            if (!narrowedRules.isEmpty()) {
                Map<MethodDescription, MethodHookConfiguration> result = new HashMap<>();
                for (MethodDescription method : type.getDeclaredMethods()) {
                    Set<InstrumentationRule> rulesMatchingOnMethod = narrowedRules.stream()
                            .filter(rule -> rule.getScopes()
                                    .stream()
                                    .anyMatch(scope -> scope.getMethodMatcher().matches(method)))
                            .collect(Collectors.toSet());
                    if (!rulesMatchingOnMethod.isEmpty()) {
                        try {
                            Set<InstrumentationRule> matchedAndIncludedRules = resolveIncludes(config, rulesMatchingOnMethod);
                            result.put(method, hookResolver.buildHookConfiguration(config, matchedAndIncludedRules));
                        } catch (Exception e) {
                            log.error("Could not build hook for {} of class {}", CoreUtils.getSignature(method), typeWithLoader.getName(), e);
                        }
                    }
                }
                return result;
            }
        } catch (NoClassDefFoundError e) {
            //the class contains a reference to an not loadable class
            //this the case for example for very many spring boot classes
            log.trace("Ignoring class {} for hooking as it is not initializable ", typeWithLoader.getName(), e);
        }
        return Collections.emptyMap();
    }

    /**
     * Finds out for each method of the given class which rules apply and builds a {@link MethodHookConfiguration} for each instrumented method.
     *
     * @param clazz the class to check
     *
     * @return a map mapping hook configurations to the methods which they should be applied on.
     */
    public Map<MethodDescription, MethodHookConfiguration> getHookConfigurations(Class<?> clazz) {
        return getHookConfigurations(TypeDescriptionWithClassLoader.of(clazz));

    }

    /**
     * For a given collection of rules, a set containing these rules and all rules included (transitively) by them are returned.
     *
     * @param config the configuration which is used to resolve rule names to rules
     * @param rules  the initial collection of rules
     *
     * @return the set of the initial rules plus their includes
     */
    @VisibleForTesting
    Set<InstrumentationRule> resolveIncludes(InstrumentationConfiguration config, Collection<InstrumentationRule> rules) {
        Set<InstrumentationRule> result = new HashSet<>();
        for (InstrumentationRule rootRule : rules) {
            addWithIncludes(rootRule, config, result);
        }
        return result;
    }

    private void addWithIncludes(InstrumentationRule rule, InstrumentationConfiguration config, Set<InstrumentationRule> result) {
        if (result.add(rule)) {
            for (String includedRuleName : rule.getIncludedRuleNames()) {
                config.getRuleByName(includedRuleName)
                        .ifPresent(includedRule -> addWithIncludes(includedRule, config, result));
            }
        }
    }

    /**
     * Narrows a rule for a specific type. The rules existing in the returned set are containing only {@link InstrumentationScope}s
     * which are matching for the given type. This prevents that method matchers will be applied to the wrong types.
     *
     * @param typeDescription the class which are the rules targeting
     * @param config          the configuration which is used as basis for the rules
     *
     * @return Returns a set containing rules with scopes targeting only the given type.
     */
    private Set<InstrumentationRule> getNarrowedRulesFor(TypeDescription typeDescription, InstrumentationConfiguration config) {
        return config.getRules()
                .stream()
                .map(rule -> Pair.of(rule, rule.getScopes()
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
        InstrumentationConfiguration configuration = InstrumentationConfiguration.builder()
                .metricsEnabled(config.getMetrics().isEnabled())
                .tracingEnabled(config.getTracing().isEnabled())
                .tracingSettings(config.getTracing())
                .source(config.getInstrumentation())
                .rules(ruleResolver.resolve(config.getInstrumentation(), genericActions))
                .propagationMetaData(propagationMetaDataResolver.resolve(config))
                .build();

        if (log.isDebugEnabled()) {
            RuleDependencyTreePrinter printer = new RuleDependencyTreePrinter(configuration.getRules());
            log.debug(printer.toString());
        }

        return configuration;
    }

    /**
     * Checks if the given class should not be instrumented based on the given configuration.
     *
     * @param typeWithLoader the {@link  java.lang.reflect.Type} to check
     * @param config         configuration to check for
     *
     * @return true, if the class is ignored (=it should not be instrumented)
     */
    @VisibleForTesting
    boolean isIgnoredClass(TypeDescriptionWithClassLoader typeWithLoader, InstrumentationConfiguration config) {

        ClassLoader loader = typeWithLoader.getLoader();
        TypeDescription type = typeWithLoader.getType();

        if (type.isPrimitive() || type.isArray()) {
            return true;
        }

        if (type.isAssignableTo(DoNotInstrumentMarker.class)) {
            return true;
        }

        if (loader != null && DoNotInstrumentMarker.class.isAssignableFrom(loader.getClass())) {
            return true;
        }

        if (loader == INSPECTIT_CLASSLOADER) {
            return true;
        }

        if (config.getSource().isExcludeLambdas() && type.getName().contains("$$Lambda")) {
            return true;
        }
        return isClassFromIgnoredPackage(config.getSource(), type.getName(), loader);
    }

    public static boolean isClassFromIgnoredPackage(InstrumentationSettings settings, String className, ClassLoader loader) {
        boolean isIgnored = settings.getIgnoredPackages()
                .entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .anyMatch(e -> className.startsWith(e.getKey()));
        if (isIgnored) {
            return true;
        }

        if (loader == null) {
            return settings.getIgnoredBootstrapPackages()
                    .entrySet()
                    .stream()
                    .filter(Map.Entry::getValue)
                    .anyMatch(e -> className.startsWith(e.getKey()));
        }
        return false;
    }
}

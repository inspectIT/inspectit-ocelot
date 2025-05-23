package rocks.inspectit.ocelot.core.selfmonitoring.instrumentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.annotations.VisibleForTesting;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.InstrumentationFeedbackSettings;
import rocks.inspectit.ocelot.core.instrumentation.hook.HookManager;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Collects the currently APPLIED instrumentation. The applied instrumentation does not always have to match with the
 * configured instrumentation, thus we can use this service to get feedback about the current state.
 * <br>
 * The service will ALWAYS include instrumented classes, when feedback is requested.
 * Additionally, for each class the instrumented methods and / or the particular rules, which cause the
 * instrumentation can be included.
 */
@Component
@Slf4j
public class InstrumentationFeedbackService extends DynamicallyActivatableService {

    /**
     * If methods should not be included, but rules, use this placeholder, because we need at least one method
     */
    @VisibleForTesting
    static final String NO_METHODS_PLACEHOLDER = "_rules";

    private static final String EMPTY = "{}";

    @Autowired
    private HookManager hookManager;

    private final ObjectMapper mapper;

    private boolean includeMethods = true;

    private boolean includeRules = true;

    /** additional boolean for testing, should always be the same as {@link #enabled} */
    private boolean isActive = false;

    public InstrumentationFeedbackService() {
        super("instrumentationFeedback");
        mapper = initializeMapper();
    }

    /**
     * Get the currently instrumented classes. Additionally, the instrumented methods and/or the particular rules
     * might be included for each class.
     *
     * @return the currently applied instrumentation as JSON string
     */
    public String getInstrumentation() {
        if(!isActive) return EMPTY;

        Map<Class<?>, Map<String, MethodHook>> activeHooks = hookManager.getHooks();
        List<ClassInstrumentation> instrumentationFeedback = new LinkedList<>();

        activeHooks.forEach((clazz, methodHookMap) -> {
            Map<String, List<String>> details = resolveInstrumentationDetails(methodHookMap);
            ClassInstrumentation classInstrumentation = new ClassInstrumentation(clazz.getName(), details);
            instrumentationFeedback.add(classInstrumentation);
        });

        try {
            return mapper.writeValueAsString(instrumentationFeedback);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize instrumentation feedback: {}", e.getMessage());
            return EMPTY;
        }
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        return configuration.getInstrumentationFeedback() != null && configuration.getInstrumentationFeedback().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        InstrumentationFeedbackSettings settings = configuration.getInstrumentationFeedback();

        if (settings != null) {
            log.info("Enabling InstrumentationFeedback");
            includeMethods = settings.isIncludeMethods();
            includeRules = settings.isIncludeRules();
            isActive = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling InstrumentationFeedback");
        isActive = false;
        return true;
    }

    /**
     * @param methodHookMap the map of method signatures and {@link MethodHook}s
     *
     * @return the resolved instrumentation details for a particular class
     */
    private Map<String, List<String>> resolveInstrumentationDetails(Map<String, MethodHook> methodHookMap) {
        Map<String, List<String>> classInstrumentation = new HashMap<>();

        // fill the classInstrumentation according to the current settings
        if (includeOnlyClasses()) return classInstrumentation;

        else if (includeJustMethods()) {
            methodHookMap.keySet()
                    .forEach(method -> classInstrumentation.put(method, Collections.emptyList()));
        }

        else if (includeJustRules()) {
            List<String> matchedRules = methodHookMap.values()
                    .stream()
                    .map(methodHook -> methodHook.getSourceConfiguration().getMatchedRulesNames())
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());

            classInstrumentation.put(NO_METHODS_PLACEHOLDER, matchedRules);
        }

        else {
            methodHookMap.forEach((method, methodHook) -> {
                List<String> matchedRules = methodHook.getSourceConfiguration().getMatchedRulesNames();
                classInstrumentation.put(method, matchedRules);
            });
        }

        return classInstrumentation;
    }

    /**
     * @return true, if the instrumentation feedback should only contain instrumented classes
     */
    private boolean includeOnlyClasses() {
        return !includeMethods && !includeRules;
    }

    /**
     * @return true, if the instrumentation feedback should include methods without rules for each class
     */
    private boolean includeJustMethods() {
        return includeMethods && !includeRules;
    }

    /**
     * @return true, if the instrumentation feedback should include rules without methods for each class
     */
    private boolean includeJustRules() {
        return includeRules && !includeMethods;
    }

    private ObjectMapper initializeMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @VisibleForTesting
    static class ClassInstrumentation {

        /**
         * The name of the instrumented class
         */
        String instrumentedClass;

        /**
         * The collection of instrumented methods with their particular rules
         */
        Map<String, List<String>> details;
    }
}

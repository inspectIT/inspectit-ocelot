package rocks.inspectit.ocelot.core.selfmonitoring.instrumentation;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
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

    @Autowired
    private HookManager hookManager;

    private boolean includeMethods = true;

    private boolean includeRules = true;

    /** additional boolean for testing, should always be the same as {@link #enabled} */
    private boolean isActive = false;

    public InstrumentationFeedbackService() {
        super("instrumentationFeedback");
    }

    /**
     * Get the collection of instrumented classes. Additionally, the instrumented methods and/or the particular rules
     * might be included for each class.
     *
     * @return the currently applied instrumentation
     */
    public Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> getInstrumentation() {
        if(!isActive) return Collections.emptyMap();

        Map<Class<?>, Map<String, MethodHook>> activeHooks = hookManager.getHooks();

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> instrumentationFeedback = new HashMap<>();

        activeHooks.forEach((clazz, methodHookMap) -> {
            InstrumentationFeedbackCommand.ClassInstrumentation classInstrumentation = resolveClassInstrumentation(methodHookMap);
            instrumentationFeedback.put(clazz.getName(), classInstrumentation);
        });

        return instrumentationFeedback;
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
     * @return the resolved {@link InstrumentationFeedbackCommand.ClassInstrumentation}
     */
    private InstrumentationFeedbackCommand.ClassInstrumentation resolveClassInstrumentation(Map<String, MethodHook> methodHookMap) {
        Map<String, List<String>> methodInstrumentationFeedback = new HashMap<>();
        val classInstrumentation = new InstrumentationFeedbackCommand.ClassInstrumentation(methodInstrumentationFeedback);

        // fill the methodInstrumentationFeedback according to the current settings
        if (includeOnlyClasses()) return classInstrumentation;

        else if (includeJustMethods()) {
            methodHookMap.keySet()
                    .forEach(method -> methodInstrumentationFeedback.put(method, Collections.emptyList()));
        }

        else if (includeJustRules()) {
            List<String> matchedRules = methodHookMap.values()
                    .stream()
                    .map(methodHook -> methodHook.getSourceConfiguration().getMatchedRulesNames())
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());

            methodInstrumentationFeedback.put(NO_METHODS_PLACEHOLDER, matchedRules);
        }

        else {
            methodHookMap.forEach((method, methodHook) -> {
                List<String> matchedRules = methodHook.getSourceConfiguration().getMatchedRulesNames();
                methodInstrumentationFeedback.put(method, matchedRules);
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
}

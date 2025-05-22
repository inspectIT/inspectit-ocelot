package rocks.inspectit.ocelot.core.selfmonitoring.instrumentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.commons.models.command.impl.InstrumentationFeedbackCommand;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.selfmonitoring.InstrumentationFeedbackSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.hook.HookManager;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static rocks.inspectit.ocelot.core.selfmonitoring.instrumentation.InstrumentationFeedbackService.NO_METHODS_PLACEHOLDER;

@ExtendWith(MockitoExtension.class)
public class InstrumentationFeedbackServiceTest {

    @InjectMocks
    private InstrumentationFeedbackService service;

    @Mock
    private HookManager hookManager;

    private static final String method1 = "method1";

    private static final String method2 = "method2";

    private static List<String> rules1;

    private static List<String> rules2;

    @BeforeEach
    void setup() {
        rules1 = new LinkedList<>();
        rules1.add("rule1");
        rules2 = new LinkedList<>();
        rules2.add("rule1");
        rules2.add("rule2");

        Map<Class<?>, Map<String, MethodHook>> hooks = createHooks();
        lenient().when(hookManager.getHooks()).thenReturn(hooks);
    }

    @Test
    void shouldCollectInstrumentationWithMethodsAndRules() {
        service.doEnable(createConfig(true, true));

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> result = service.getInstrumentation();

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(InstrumentationFeedbackServiceTest.class.getName());
        Map<String, List<String>> classInstrumentation =
                result.values().iterator().next().getClassInstrumentation();
        assertThat(classInstrumentation).containsEntry(method1, rules1);
        assertThat(classInstrumentation).containsEntry(method2, rules2);
    }

    @Test
    void shouldCollectInstrumentationWithJustMethods() {
        service.doEnable(createConfig(true, false));

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> result = service.getInstrumentation();

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(InstrumentationFeedbackServiceTest.class.getName());
        Map<String, List<String>> classInstrumentation =
                result.values().iterator().next().getClassInstrumentation();
        assertThat(classInstrumentation).containsEntry(method1, Collections.emptyList());
        assertThat(classInstrumentation).containsEntry(method2, Collections.emptyList());
    }

    @Test
    void shouldCollectInstrumentationWithJustRules() {
        service.doEnable(createConfig(false, true));

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> result = service.getInstrumentation();

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(InstrumentationFeedbackServiceTest.class.getName());
        Map<String, List<String>> classInstrumentation =
                result.values().iterator().next().getClassInstrumentation();
        assertThat(classInstrumentation).containsEntry(NO_METHODS_PLACEHOLDER, rules2);
    }

    @Test
    void shouldCollectInstrumentationWithJustClasses() {
        service.doEnable(createConfig(false, false));

        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> result = service.getInstrumentation();

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(InstrumentationFeedbackServiceTest.class.getName());
        Map<String, List<String>> classInstrumentation =
                result.values().iterator().next().getClassInstrumentation();
        assertThat(classInstrumentation).isEmpty();
    }

    @Test
    void shouldCollectNothingWhenNotEnabled() {
        Map<String, InstrumentationFeedbackCommand.ClassInstrumentation> result = service.getInstrumentation();

        assertThat(result).isEmpty();
    }

    private static InspectitConfig createConfig(boolean includeMethods, boolean includeRules) {
        InstrumentationFeedbackSettings settings = new InstrumentationFeedbackSettings();
        settings.setEnabled(true);
        settings.setIncludeMethods(includeMethods);
        settings.setIncludeRules(includeRules);

        InspectitConfig config = new InspectitConfig();
        config.setInstrumentationFeedback(settings);
        return config;
    }

    /**
     * Creates method hooks for one class, with two methods.
     * The first method is instrumented by one rule. The second method is instrumented by two rules
     */
    private static Map<Class<?>, Map<String, MethodHook>> createHooks() {
        Map<String, MethodHook> methodHooks = createMethodHooks(method1, rules1);
        Map<String, MethodHook> moreMethodHooks = createMethodHooks(method2, rules2);
        methodHooks.putAll(moreMethodHooks);

        Map<Class<?>, Map<String, MethodHook>> hooks = new HashMap<>();
        hooks.put(InstrumentationFeedbackServiceTest.class, methodHooks);

        return hooks;
    }

    private static Map<String, MethodHook> createMethodHooks(String methodName, List<String> ruleNames) {
        Map<String, MethodHook> methodHooks = new HashMap<>();
        MethodHookConfiguration methodHookConfig = MethodHookConfiguration.builder().matchedRulesNames(ruleNames).build();
        MethodHook methodHook = MethodHook.builder()
                .sourceConfiguration(methodHookConfig)
                .actionScopeFactory(mock(ActionScopeFactory.class))
                .build();
        methodHooks.put(methodName, methodHook);

        return methodHooks;
    }
}

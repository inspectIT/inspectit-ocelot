package rocks.inspectit.ocelot.core.instrumentation.hook;

import io.opencensus.stats.StatsRecorder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.GenericActionSettings;
import rocks.inspectit.ocelot.config.utils.ConfigUtils;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodTracingConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.genericactions.BoundGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.genericactions.GenericActionGenerator;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.*;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * This class is responsible for translating {@link MethodHookConfiguration}s
 * into executable {@link MethodHook}s.
 */
@Component
@Slf4j
public class MethodHookGenerator {

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private MeasuresAndViewsManager metricsManager;

    @Autowired
    private StatsRecorder statsRecorder;

    @Autowired
    private GenericActionGenerator genericActionGenerator;

    /**
     * Builds a executable method hook based on the given configuration.
     *
     * @param declaringClass teh class defining the method which is being hooked
     * @param method         a method descriptor of the hooked method
     * @param config         the configuration to use for building the hook
     * @return the generated method hook
     */
    public MethodHook buildHook(Class<?> declaringClass, MethodDescription method, MethodHookConfiguration config) {
        val builder = MethodHook.builder()
                .inspectitContextManager(contextManager)
                .sourceConfiguration(config);

        val methodInfo = MethodReflectionInformation.createFor(declaringClass, method);
        builder.methodInformation(methodInfo);

        val entryActions = new CopyOnWriteArrayList<IHookAction>();
        entryActions.addAll(buildActionCalls(config.getEntryActions(), methodInfo));
        buildTracingEntryAction(config.getTracing())
                .ifPresent(entryActions::add);
        builder.entryActions(entryActions);

        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        exitActions.addAll(buildActionCalls(config.getExitActions(), methodInfo));
        buildTracingExitAction(config.getTracing())
                .ifPresent(exitActions::add);
        buildMetricsRecorder(config)
                .ifPresent(exitActions::add);
        builder.exitActions(exitActions);

        return builder.build();
    }


    private Optional<IHookAction> buildTracingExitAction(MethodTracingConfiguration tracing) {
        val attributes = tracing.getAttributes();
        if (!attributes.isEmpty()) {
            IHookAction endTraceAction = new WriteSpanAttributesAction(attributes);
            val actionWithConditions = ConditionalHookAction.wrapWithConditionChecks(tracing.getAttributeConditions(), endTraceAction);
            return Optional.of(actionWithConditions);
        } else {
            return Optional.empty();
        }
    }

    private Optional<IHookAction> buildTracingEntryAction(MethodTracingConfiguration tracing) {
        if (tracing.isStartSpan()) {
            IHookAction beginTraceAction = new StartSpanAction(tracing.getSpanNameDataKey(), tracing.getSpanKind());
            val actionWithConditions = ConditionalHookAction.wrapWithConditionChecks(tracing.getStartSpanConditions(), beginTraceAction);
            return Optional.of(actionWithConditions);
        } else {
            return Optional.empty();
        }
    }

    private Optional<IHookAction> buildMetricsRecorder(MethodHookConfiguration config) {
        if (!config.getConstantMetrics().isEmpty() || !config.getDataMetrics().isEmpty()) {
            val recorder = new MetricsRecorder(config.getConstantMetrics(), config.getDataMetrics(), metricsManager, statsRecorder);
            return Optional.of(recorder);
        } else {
            return Optional.empty();
        }
    }

    private List<IHookAction> buildActionCalls(List<ActionCallConfig> calls, MethodReflectionInformation methodInfo) {

        List<IHookAction> result = new ArrayList<>();
        for (val call : calls) {
            try {
                result.add(generateAndBindGenericAction(methodInfo, call));
            } catch (Exception e) {
                log.error("Failed to build action {} for data {} on method {}, no value will be assigned",
                        call.getAction().getName(), call.getName(), methodInfo.getMethodFQN(), e);
            }
        }
        return result;
    }

    /**
     * Generates a action and binds its arguments.
     *
     * @param methodInfo       the method in which this action will be used.
     * @param actionCallConfig the specification of the call to the data action
     * @return the executable generic action
     */
    private IHookAction generateAndBindGenericAction(MethodReflectionInformation methodInfo, ActionCallConfig actionCallConfig) {
        GenericActionConfig actionConfig = actionCallConfig.getAction();
        val callSettings = actionCallConfig.getCallSettings();
        val injectedActionClass = genericActionGenerator.getOrGenerateGenericAction(actionConfig, methodInfo.getDeclaringClass());

        val dynamicAssignments = getDynamicInputAssignments(methodInfo, actionCallConfig);
        val constantAssignments = getConstantInputAssignments(methodInfo, actionCallConfig);

        IHookAction actionCall = BoundGenericAction.bind(actionCallConfig.getName(), actionConfig, injectedActionClass, constantAssignments, dynamicAssignments);

        return ConditionalHookAction.wrapWithConditionChecks(callSettings, actionCall);
    }

    /**
     * Reads the constant assignments performed by the given action call into a map.
     * The data is immediately converted to the expected input type using a conversion service.
     *
     * @param methodInfo       the method within which the action is executed, used to find the correct types
     * @param actionCallConfig the call whose constant assignments should be queried
     * @return a map mapping the name of the parameters to the constant value they are assigned
     */
    private Map<String, Object> getConstantInputAssignments(MethodReflectionInformation methodInfo, ActionCallConfig actionCallConfig) {
        GenericActionConfig actionConfig = actionCallConfig.getAction();
        Map<String, Object> constantAssignments = new HashMap<>();

        ActionCallSettings callSettings = actionCallConfig.getCallSettings();
        SortedMap<String, String> actionArgumentTypes = actionConfig.getAdditionalArgumentTypes();
        actionCallConfig.getCallSettings().getConstantInput()
                .forEach((argName, value) -> {
                    String expectedTypeName = actionArgumentTypes.get(argName);
                    ClassLoader contextClassloader = methodInfo.getDeclaringClass().getClassLoader();
                    Class<?> expectedValueType = ConfigUtils.locateTypeWithinImports(expectedTypeName, contextClassloader, actionConfig.getImportedPackages());
                    Object convertedValue = callSettings.getConstantInputAsType(argName, expectedValueType);
                    constantAssignments.put(argName, convertedValue);
                });
        if (actionArgumentTypes.containsKey(GenericActionSettings.METHOD_NAME_VARIABLE)) {
            constantAssignments.put(GenericActionSettings.METHOD_NAME_VARIABLE, methodInfo.getName());
        }
        return constantAssignments;
    }

    /**
     * Reads the dynamic assignments performed by the given action call into a map.
     * Currently the only dynamic assignments are "data-inputs".
     *
     * @param methodInfo       the method within which the action is executed, used to assign special variables
     * @param actionCallConfig the call whose dynamic assignments should be queried
     * @return a map mapping the parameter names to functions which are evaluated during
     * {@link IHookAction#execute(IHookAction.ExecutionContext)}  to find the concrete value for the parameter.
     */
    private Map<String, Function<IHookAction.ExecutionContext, Object>> getDynamicInputAssignments(MethodReflectionInformation methodInfo, ActionCallConfig actionCallConfig) {
        Map<String, Function<IHookAction.ExecutionContext, Object>> dynamicAssignments = new HashMap<>();
        actionCallConfig.getCallSettings().getDataInput()
                .forEach((argName, dataName) ->
                        dynamicAssignments.put(argName, (ctx) -> ctx.getInspectitContext().getData(dataName))
                );
        val additionalInputVars = actionCallConfig.getAction().getAdditionalArgumentTypes().keySet();
        if (additionalInputVars.contains(GenericActionSettings.METHOD_PARAMETER_TYPES_VARIABLE)) {
            dynamicAssignments.put(GenericActionSettings.METHOD_PARAMETER_TYPES_VARIABLE, (ex) -> methodInfo.getParameterTypes());
        }
        if (additionalInputVars.contains(GenericActionSettings.CLASS_VARIABLE)) {
            dynamicAssignments.put(GenericActionSettings.CLASS_VARIABLE, (ex) -> methodInfo.getDeclaringClass());
        }
        return dynamicAssignments;
    }

}

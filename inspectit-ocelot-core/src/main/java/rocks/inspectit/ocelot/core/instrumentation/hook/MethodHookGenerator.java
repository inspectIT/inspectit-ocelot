package rocks.inspectit.ocelot.core.instrumentation.hook;

import io.opencensus.stats.StatsRecorder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.DataProviderCallSettings;
import rocks.inspectit.ocelot.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.DataProviderCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericDataProviderConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodTracingConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.dataprovider.generic.BoundDataProvider;
import rocks.inspectit.ocelot.core.instrumentation.dataprovider.generic.DataProviderGenerator;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.*;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.utils.CommonUtils;

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
    private DataProviderGenerator dataProviderGenerator;

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
        entryActions.addAll(buildDataProviderCalls(config.getEntryProviders(), methodInfo));
        buildTracingEntryAction(config.getTracing())
                .ifPresent(entryActions::add);
        builder.entryActions(entryActions);

        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        exitActions.addAll(buildDataProviderCalls(config.getExitProviders(), methodInfo));
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
            val actionWithConditions = ConditionalHookAction.wrapWithConditionChecks(tracing.getConditions(), endTraceAction);
            return Optional.of(actionWithConditions);
        } else {
            return Optional.empty();
        }
    }

    private Optional<IHookAction> buildTracingEntryAction(MethodTracingConfiguration tracing) {
        if (tracing.isStartSpan()) {
            IHookAction beginTraceAction = new StartSpanAction(tracing.getSpanNameDataKey(), tracing.getSpanKind());
            val actionWithConditions = ConditionalHookAction.wrapWithConditionChecks(tracing.getConditions(), beginTraceAction);
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

    private List<IHookAction> buildDataProviderCalls(List<DataProviderCallConfig> calls, MethodReflectionInformation methodInfo) {

        List<IHookAction> result = new ArrayList<>();
        for (val call : calls) {
            try {
                result.add(generateAndBindDataProvider(methodInfo, call));
            } catch (Exception e) {
                log.error("Failed to build data provider {} for data {} on method {}, no value will be assigned",
                        call.getProvider().getName(), call.getName(), methodInfo.getMethodFQN(), e);
            }
        }
        return result;
    }

    /**
     * Generates a data provider and binds its arguments.
     *
     * @param methodInfo         the method in which this data provider will be used.
     * @param providerCallConfig the specification of the call to the data provider
     * @return the executable data provider
     */
    private IHookAction generateAndBindDataProvider(MethodReflectionInformation methodInfo, DataProviderCallConfig providerCallConfig) {
        GenericDataProviderConfig providerConfig = providerCallConfig.getProvider();
        val callSettings = providerCallConfig.getCallSettings();
        val injectedProviderClass = dataProviderGenerator.getOrGenerateDataProvider(providerConfig, methodInfo.getDeclaringClass());

        val dynamicAssignments = getDynamicInputAssignments(methodInfo, providerCallConfig);
        val constantAssignments = getConstantInputAssignments(methodInfo, providerCallConfig);

        IHookAction providerCall = BoundDataProvider.bind(providerCallConfig.getName(), providerConfig, injectedProviderClass, constantAssignments, dynamicAssignments);

        return ConditionalHookAction.wrapWithConditionChecks(callSettings, providerCall);
    }

    /**
     * Reads the constant assignments performed by the given provider call into a map.
     * The data is immediately converted to the expected input type using a conversion service.
     *
     * @param methodInfo         the method within which the data provider is executed, used to find the correct types
     * @param providerCallConfig the call whose constant assignments should be queried
     * @return a map mapping the name of the parameters to the constant value they are assigned
     */
    private Map<String, Object> getConstantInputAssignments(MethodReflectionInformation methodInfo, DataProviderCallConfig providerCallConfig) {
        GenericDataProviderConfig providerConfig = providerCallConfig.getProvider();
        Map<String, Object> constantAssignments = new HashMap<>();

        DataProviderCallSettings callSettings = providerCallConfig.getCallSettings();
        SortedMap<String, String> providerArgumentTypes = providerConfig.getAdditionalArgumentTypes();
        providerCallConfig.getCallSettings().getConstantInput()
                .forEach((argName, value) -> {
                    String expectedTypeName = providerArgumentTypes.get(argName);
                    ClassLoader contextClassloader = methodInfo.getDeclaringClass().getClassLoader();
                    Class<?> expectedValueType = CommonUtils.locateTypeWithinImports(expectedTypeName, contextClassloader, providerConfig.getImportedPackages());
                    Object convertedValue = callSettings.getConstantInputAsType(argName, expectedValueType);
                    constantAssignments.put(argName, convertedValue);
                });
        if (providerArgumentTypes.containsKey(GenericDataProviderSettings.METHOD_NAME_VARIABLE)) {
            constantAssignments.put(GenericDataProviderSettings.METHOD_NAME_VARIABLE, methodInfo.getName());
        }
        return constantAssignments;
    }

    /**
     * Reads the dynamic assignments performed by the given provider call into a map.
     * Currently the only dynamic assignments are "data-inputs".
     *
     * @param methodInfo         the method within which the data provider is executed, used to assign special variables
     * @param providerCallConfig the call whose dynamic assignments should be queried
     * @return a map mapping the parameter names to functions which are evaluated during
     * {@link IHookAction#execute(IHookAction.ExecutionContext)}  to find the concrete value for the parameter.
     */
    private Map<String, Function<IHookAction.ExecutionContext, Object>> getDynamicInputAssignments(MethodReflectionInformation methodInfo, DataProviderCallConfig providerCallConfig) {
        Map<String, Function<IHookAction.ExecutionContext, Object>> dynamicAssignments = new HashMap<>();
        providerCallConfig.getCallSettings().getDataInput()
                .forEach((argName, dataName) ->
                        dynamicAssignments.put(argName, (ctx) -> ctx.getInspectitContext().getData(dataName))
                );
        val additionalProviderInputVars = providerCallConfig.getProvider().getAdditionalArgumentTypes().keySet();
        if (additionalProviderInputVars.contains(GenericDataProviderSettings.METHOD_PARAMETER_TYPES_VARIABLE)) {
            dynamicAssignments.put(GenericDataProviderSettings.METHOD_PARAMETER_TYPES_VARIABLE, (ex) -> methodInfo.getParameterTypes());
        }
        if (additionalProviderInputVars.contains(GenericDataProviderSettings.CLASS_VARIABLE)) {
            dynamicAssignments.put(GenericDataProviderSettings.CLASS_VARIABLE, (ex) -> methodInfo.getDeclaringClass());
        }
        return dynamicAssignments;
    }

}

package rocks.inspectit.oce.core.instrumentation.hook;

import io.opencensus.stats.StatsRecorder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.DataProviderCallSettings;
import rocks.inspectit.oce.core.config.model.instrumentation.dataproviders.GenericDataProviderSettings;
import rocks.inspectit.oce.core.instrumentation.config.model.DataProviderCallConfig;
import rocks.inspectit.oce.core.instrumentation.config.model.GenericDataProviderConfig;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.BoundDataProvider;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;
import rocks.inspectit.oce.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.oce.core.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * This class is responsible for translating {@link rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration}s
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
        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        addDataProviderCalls(methodInfo, config, entryActions, exitActions);

        addMetricsRecorder(config, exitActions);

        builder.entryActions(entryActions);
        builder.exitActions(exitActions);

        return builder.build();
    }

    private void addMetricsRecorder(MethodHookConfiguration config, CopyOnWriteArrayList<IHookAction> exitActions) {
        if (!config.getConstantMetrics().isEmpty() || !config.getDataMetrics().isEmpty()) {

            val recorder = new MetricsRecorder(config.getConstantMetrics(), config.getDataMetrics(), metricsManager, statsRecorder);
            exitActions.add(recorder);
        }
    }

    /**
     * Adds the entry and exit calls to data providers as hook actions to the hook.
     *
     * @param method       the information about the method within which this data provider is called
     * @param config       the configuration of the method hook specifying the data providers to call
     * @param entryActions the list of entry hook actions to which the data providers will be appended
     * @param exitActions  the list of exit hook actions to which the data providers will be appended
     */
    private void addDataProviderCalls(MethodReflectionInformation method, MethodHookConfiguration config, List<IHookAction> entryActions, List<IHookAction> exitActions) {
        config.getEntryProviders().forEach(pair -> {
            String dataKey = pair.getLeft();
            val providerCallConfig = pair.getRight();
            try {
                BoundDataProvider call = generateAndBindDataProvider(method, dataKey, providerCallConfig);
                entryActions.add(call);
            } catch (Exception e) {
                log.error("Failed to build entry data provider {} for data {} on method {}, no value will be assigned",
                        providerCallConfig.getProvider().getName(), dataKey, method.getMethodFQN(), e);
            }
        });

        config.getExitProviders().forEach(pair -> {
            String dataKey = pair.getLeft();
            val providerCallConfig = pair.getRight();
            try {
                BoundDataProvider call = generateAndBindDataProvider(method, dataKey, providerCallConfig);
                exitActions.add(call);
            } catch (Exception e) {
                log.error("Failed to build exit data provider {} for data {} on method {}, no value will be assigned",
                        providerCallConfig.getProvider().getName(), dataKey, method.getMethodFQN(), e);
            }
        });
    }

    /**
     * Generates a data provider and binds its arguments.
     *
     * @param methodInfo         the method in which this data provider will be used.
     * @param dataKey            the name of the data whose value is defined by executing the given data provider
     * @param providerCallConfig the specification of the call to the data provider
     * @return the executable data provider
     */
    private BoundDataProvider generateAndBindDataProvider(MethodReflectionInformation methodInfo, String dataKey, DataProviderCallConfig providerCallConfig) {
        GenericDataProviderConfig providerConfig = providerCallConfig.getProvider();
        val injectedProviderClass = dataProviderGenerator.getOrGenerateDataProvider(providerConfig, methodInfo.getDeclaringClass());

        val dynamicAssignments = getDynamicInputAssignments(methodInfo, providerCallConfig);
        val constantAssignments = getConstantInputAssignments(methodInfo, providerCallConfig);

        return BoundDataProvider.bind(dataKey, providerConfig, injectedProviderClass, constantAssignments, dynamicAssignments);
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

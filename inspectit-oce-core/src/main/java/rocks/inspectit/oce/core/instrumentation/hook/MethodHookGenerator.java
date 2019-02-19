package rocks.inspectit.oce.core.instrumentation.hook;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.instrumentation.config.model.DataProviderCallConfig;
import rocks.inspectit.oce.core.instrumentation.config.model.GenericDataProviderConfig;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.BoundDataProvider;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;
import rocks.inspectit.oce.core.utils.AutoboxingHelper;
import rocks.inspectit.oce.core.utils.CommonUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

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
    private DataProviderGenerator dataProviderGenerator;

    private ConversionService conversionService = ApplicationConversionService.getSharedInstance();

    /**
     * Builds a executable method hook based on the given configuration.
     *
     * @param declaringClass teh class defining the method which is being hooked
     * @param method         a method descriptor of the hooked method
     * @param config         the configuration to use for building the hook
     * @return the generated method hook
     */
    public MethodHook buildHook(Class<?> declaringClass, MethodDescription method, MethodHookConfiguration config) {
        String signature = CommonUtils.getSignature(method);
        val builder = MethodHook.builder()
                .inspectitContextManager(contextManager)
                .methodName(CommonUtils.getSignature(method))
                .sourceConfiguration(config);

        addReflectionInformationToHook(declaringClass, method, builder);

        val entryActions = new CopyOnWriteArrayList<IHookAction>();
        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        addDataProviderCalls(declaringClass, signature, config, entryActions, exitActions);


        entryActions.add(new IHookAction() {
            @Override
            public void execute(IHookAction.ExecutionContext ctx) {
                if (log.isTraceEnabled()) {
                    log.trace("###Entering {}", ctx.getHook().getMethodName());
                    ctx.getInspectitContext().getData().forEach(e -> log.trace("###   {}={}", e.getKey(), e.getValue()));
                }
            }

            @Override
            public String getName() {
                return "Enter-print";
            }
        });

        exitActions.add(new IHookAction() {
            @Override
            public void execute(IHookAction.ExecutionContext ctx) {
                if (log.isTraceEnabled()) {
                    log.trace("###exiting {}", ctx.getHook().getMethodName());
                    ctx.getInspectitContext().getData().forEach(e -> log.trace("###   {}={}", e.getKey(), e.getValue()));
                }
            }

            @Override
            public String getName() {
                return "Exit-print";
            }
        });

        builder.entryActions(entryActions);
        builder.exitActions(exitActions);

        return builder.build();
    }

    /**
     * Adds the entry and exit calls to data providers as hook actions to the hook.
     *
     * @param hookedClass  the class of which the method is being hooked
     * @param signature    the signature of the method being hooked, only used to print meaningful error messages
     * @param config       the configuration of the method hook specifying the data providers to call
     * @param entryActions the list of entry hook actions to which the data providers will be appended
     * @param exitActions  the list of exit hook actions to which the data providers will be appended
     */
    private void addDataProviderCalls(Class<?> hookedClass, String signature, MethodHookConfiguration config, List<IHookAction> entryActions, List<IHookAction> exitActions) {
        config.getEntryProviders().forEach(pair -> {
            String dataKey = pair.getLeft();
            val providerCallConfig = pair.getRight();
            try {
                BoundDataProvider call = generateAndBindDataProvider(hookedClass, dataKey, providerCallConfig);
                entryActions.add(call);
            } catch (Exception e) {
                log.error("Failed to build entry data provider {} for data {} on method {} of {}, no value will be assigned",
                        providerCallConfig.getProvider().getName(), dataKey, signature, hookedClass.getName(), e);
            }
        });

        config.getExitProviders().forEach(pair -> {
            String dataKey = pair.getLeft();
            val providerCallConfig = pair.getRight();
            try {
                BoundDataProvider call = generateAndBindDataProvider(hookedClass, dataKey, providerCallConfig);
                exitActions.add(call);
            } catch (Exception e) {
                log.error("Failed to build exit data provider {} for data {} on method {} of {}, no value will be assigned",
                        providerCallConfig.getProvider().getName(), dataKey, signature, hookedClass.getName(), e);
            }
        });
    }

    /**
     * Generates a data provider and binds its arguments.
     *
     * @param contextClass       the class in which this data provider will be used.
     * @param dataKey            the name of the data whose value is defined by executing the given data provider
     * @param providerCallConfig the specification of the call to the data provider
     * @return the executable data provider
     */
    private BoundDataProvider generateAndBindDataProvider(Class<?> contextClass, String dataKey, DataProviderCallConfig providerCallConfig) {
        GenericDataProviderConfig providerConfig = providerCallConfig.getProvider();
        val injectedProviderClass = dataProviderGenerator.getOrGenerateDataProvider(providerConfig, contextClass);

        val dynamicAssignments = getDynamicInputAssignments(providerCallConfig);
        val constantAssignments = getConstantInputAssignments(providerCallConfig, contextClass.getClassLoader());

        return BoundDataProvider.bind(dataKey, providerConfig, injectedProviderClass, constantAssignments, dynamicAssignments);
    }

    /**
     * Reads the constant assignments performed by the given provider call into a map.
     * The data is immediately converted to the expected input type using a conversion service.
     *
     * @param providerCallConfig the call whose constant assignments should be queried
     * @param context            the classloader within which the data provider is executed, used to find the correct types
     * @return a map mapping the name of the parameters to the constant value they are assigned
     */
    private Map<String, Object> getConstantInputAssignments(DataProviderCallConfig providerCallConfig, ClassLoader context) {
        GenericDataProviderConfig providerConfig = providerCallConfig.getProvider();
        Map<String, Object> constantAssignments = new HashMap<>();

        providerCallConfig.getCallSettings().getConstantInput()
                .forEach((argName, value) -> {
                    String expectedTypeName = providerConfig.getAdditionalArgumentTypes().get(argName);
                    //we don't want to give a primitive type to the conversion service
                    if (AutoboxingHelper.isPrimitiveType(expectedTypeName)) {
                        expectedTypeName = AutoboxingHelper.getWrapperForPrimitive(expectedTypeName);
                    }
                    Class<?> expectedTypeValue = CommonUtils.locateTypeWithinImports(expectedTypeName, context, providerConfig.getImportedPackages());
                    Object convertedValue = conversionService.convert(value, expectedTypeValue);
                    constantAssignments.put(argName, convertedValue);
                });
        return constantAssignments;
    }

    /**
     * Reads the dynamic assignments performed by the given provider call into a map.
     * Currently the only dynamic assignments are "data-inputs".
     *
     * @param providerCallConfig the call whose dynamic assignments should be queried
     * @return a map mapping the parameter names to functions which are evaluated during
     * {@link IHookAction#execute(IHookAction.ExecutionContext)}  to find the concrete value for the parameter.
     */
    private Map<String, Function<IHookAction.ExecutionContext, Object>> getDynamicInputAssignments(DataProviderCallConfig providerCallConfig) {
        Map<String, Function<IHookAction.ExecutionContext, Object>> dynamicAssignments = new HashMap<>();
        providerCallConfig.getCallSettings().getDataInput()
                .forEach((argName, dataName) ->
                        dynamicAssignments.put(argName, (ctx) -> ctx.getInspectitContext().getData(dataName))
                );
        return dynamicAssignments;
    }

    private void addReflectionInformationToHook(Class<?> declaringClass, MethodDescription method, MethodHook.MethodHookBuilder builder) {
        builder.hookedClass(new WeakReference<>(declaringClass));
        builder.hookedConstructor(
                Stream.of(declaringClass.getDeclaredConstructors())
                        .filter(method::represents)
                        .findFirst()
                        .map(c -> new WeakReference<Constructor<?>>(c))
                        .orElse(null));

        builder.hookedMethod(
                Stream.of(declaringClass.getDeclaredMethods())
                        .filter(method::represents)
                        .findFirst()
                        .map(m -> new WeakReference<>(m))
                        .orElse(null));
    }
}

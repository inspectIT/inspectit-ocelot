package rocks.inspectit.oce.core.instrumentation.dataprovider.generic;

import lombok.experimental.NonFinal;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.oce.bootstrap.instrumentation.IGenericDataProvider;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedGenericDataProviderConfig;
import rocks.inspectit.oce.core.instrumentation.hook.IHookAction;
import rocks.inspectit.oce.core.instrumentation.injection.InjectedClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a {@link IGenericDataProvider} which has values bound to its input arguments.
 * The bound values can be either constants or dynamically computed based on the {@link IHookAction.ExecutionContext}.
 */
@NonFinal
public abstract class BoundDataProvider implements IHookAction {

    /**
     * The data to which the value is assigned
     */
    protected final String dataKey;

    /**
     * The name of the data provider, only used to provide a meaningful name via getName()
     */
    private final String providerName;

    /**
     * Reference to the class of teh data provider.
     * This reference is held to prevent the {@link rocks.inspectit.oce.core.instrumentation.injection.ClassInjector} from reusing this provider.
     */
    private final InjectedClass<?> providerClass;

    /**
     * Reference to the actual provider instance.
     * This corresponds to the value of {@link #providerClass} for {@link GenericDataProviderTemplate#INSTANCE}.
     */
    protected final IGenericDataProvider provider;


    protected BoundDataProvider(String dataKey, ResolvedGenericDataProviderConfig providerConfig, InjectedClass<?> providerClass) {
        this.dataKey = dataKey;
        providerName = providerConfig.getName();
        this.providerClass = providerClass;
        try {
            provider = (IGenericDataProvider) providerClass.getInjectedClassObject().get().getField("INSTANCE").get(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("The given provider is not based on the GenericDatProviderTemplate");
        }
    }

    @Override
    public String getName() {
        return "DataProvider '" + providerName + "' for '" + dataKey + "'";
    }

    /**
     * Binds a data provider to the given input argument values.
     *
     * @param dataKey             the data key udner which the result of this provider will be stored
     * @param providerConfig      the configuration of the used data provider
     * @param provider            the generated provider class
     * @param constantAssignments a map mapping input variable names to their constant values
     * @param dynamicAssignments  a map mapping input variables to a function which is used to derive
     *                            the parameter value when the provider is invoked
     * @return
     */
    public static BoundDataProvider bind(String dataKey,
                                         ResolvedGenericDataProviderConfig providerConfig,
                                         InjectedClass<?> provider,
                                         Map<String, Object> constantAssignments,
                                         Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {

        if (dynamicAssignments.isEmpty()) {
            return new ConstantOnlyBoundDataProvider(dataKey, providerConfig, provider, constantAssignments);
        } else {
            return new DynamicBoundDataProvider(dataKey, providerConfig, provider, constantAssignments, dynamicAssignments);
        }
    }
}

class ConstantOnlyBoundDataProvider extends BoundDataProvider {

    private Object[] arguments;

    public ConstantOnlyBoundDataProvider(String dataKey, ResolvedGenericDataProviderConfig providerConfig,
                                         InjectedClass<?> provider, Map<String, Object> constantAssignments) {
        super(dataKey, providerConfig, provider);

        arguments = providerConfig.getAdditionalArgumentTypes().keySet().stream().map(
                constantAssignments::get
        ).toArray();
    }

    @Override
    public void execute(ExecutionContext context) {
        Object result = provider.execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), arguments);
        context.getInspectitContext().setData(dataKey, result);
    }
}

class DynamicBoundDataProvider extends BoundDataProvider {

    /**
     * A template containing the already assigned constant arguments for this data provider.
     * As the same {@link DynamicBoundDataProvider} isntance could potentially be used by multiple threads,
     * this array needs to be copied before the dynamicAssignments can be performed.
     */
    private Object[] argumentsTemplate;

    /**
     * An array containing (a) the index of the input to assign and (b) a function for defining the value.
     */
    private Pair<Integer, Function<ExecutionContext, Object>>[] dynamicAssignments;

    @SuppressWarnings("unchecked")
    public DynamicBoundDataProvider(String dataKey, ResolvedGenericDataProviderConfig providerConfig,
                                    InjectedClass<?> provider, Map<String, Object> constantAssignments,
                                    Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {
        super(dataKey, providerConfig, provider);

        int numArgs = providerConfig.getAdditionalArgumentTypes().size();
        argumentsTemplate = new Object[numArgs];

        List<Pair<Integer, Function<ExecutionContext, Object>>> dynamicAssignmentsWithIndices = new ArrayList<>();

        int idx = 0;
        val argsIterator = providerConfig.getAdditionalArgumentTypes().entrySet().iterator();
        while (argsIterator.hasNext()) {
            String argName = argsIterator.next().getKey();
            if (constantAssignments.containsKey(argName)) {
                argumentsTemplate[idx] = constantAssignments.get(argName);
            } else if (dynamicAssignments.containsKey(argName)) {
                dynamicAssignmentsWithIndices.add(Pair.of(idx, dynamicAssignments.get(argName)));
            } else {
                //should never occur as this is validated by config validations
                throw new RuntimeException("Unassigned argument!");
            }
            idx++;
        }
        this.dynamicAssignments = dynamicAssignmentsWithIndices.toArray(new Pair[0]);
    }

    @Override
    public void execute(ExecutionContext context) {
        Object[] args = Arrays.copyOf(argumentsTemplate, argumentsTemplate.length);

        for (val assignment : dynamicAssignments) {
            args[assignment.getLeft()] = assignment.getRight().apply(context);
        }

        Object result = provider.execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), args);
        context.getInspectitContext().setData(dataKey, result);
    }
}

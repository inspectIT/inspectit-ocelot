package rocks.inspectit.ocelot.core.instrumentation.dataprovider.generic;

import lombok.experimental.NonFinal;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericDataProvider;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericDataProviderConfig;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

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
     * This reference is held to prevent the {@link ClassInjector} from reusing this provider.
     */
    private final InjectedClass<?> providerClass;

    /**
     * Reference to the actual provider instance.
     * This corresponds to the value of {@link #providerClass} for {@link GenericDataProviderTemplate#INSTANCE}.
     */
    protected final IGenericDataProvider provider;


    protected BoundDataProvider(String dataKey, GenericDataProviderConfig providerConfig, InjectedClass<?> providerClass) {
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
                                         GenericDataProviderConfig providerConfig,
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

    private final Object[] arguments;

    public ConstantOnlyBoundDataProvider(String dataKey, GenericDataProviderConfig providerConfig,
                                         InjectedClass<?> provider, Map<String, Object> constantAssignments) {
        super(dataKey, providerConfig, provider);

        // the additionalArgumentTypes is a sorted map
        // the order in which the arguments appear in this map correspond to the order in which their values
        // have to be placed in the arguments array
        arguments = providerConfig.getAdditionalArgumentTypes()
                .keySet().stream()
                .map(
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
     * As the same {@link DynamicBoundDataProvider} instance could potentially be used by multiple threads,
     * this array needs to be copied before the dynamicAssignments can be performed.
     */
    private final Object[] argumentsTemplate;

    /**
     * An array containing (a) the index of the addition input to assign and (b) a function for defining the value.
     * The index corresponds to the index of the parameter in {@link GenericDataProviderConfig#getAdditionalArgumentTypes()}.
     * Therefore the index corresponds to the position in the additionalArgumetns array with which the
     * {@link IGenericDataProvider#execute(Object[], Object, Object, Throwable, Object[])} function is called.
     */
    private Pair<Integer, Function<ExecutionContext, Object>>[] dynamicAssignments;

    public DynamicBoundDataProvider(String dataKey, GenericDataProviderConfig providerConfig,
                                    InjectedClass<?> provider, Map<String, Object> constantAssignments,
                                    Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {
        super(dataKey, providerConfig, provider);

        // the sorted additionalArgumentTypes map defines the number and the order of the additional input
        // parameters the data provider expects
        // therefore we can already reserve the exact amount of space needed for the argumentsTemplate
        int numArgs = providerConfig.getAdditionalArgumentTypes().size();
        argumentsTemplate = new Object[numArgs];

        List<Pair<Integer, Function<ExecutionContext, Object>>> dynamicAssignmentsWithIndices = new ArrayList<>();

        //we now loop over the additionalArgumentTypes map and remember the index of the corresponding parameter
        //If the parameter is defined through a constant assignment we simply place it in the argumentsTemplate at the
        //index of the parameter.
        //if the parameter is defined through a dynamic assignment we cannot directly store the value already in the template.
        //Instead we remember the index and the function used to perform the assignment in dynamicAssignments.

        int idx = 0;
        for (String argName : providerConfig.getAdditionalArgumentTypes().keySet()) {
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

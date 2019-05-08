package rocks.inspectit.ocelot.core.instrumentation.genericactions;

import lombok.experimental.NonFinal;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a {@link IGenericAction} which has values bound to its input arguments.
 * The bound values can be either constants or dynamically computed based on the {@link IHookAction.ExecutionContext}.
 */
@NonFinal
public abstract class BoundGenericAction implements IHookAction {

    /**
     * The data to which the value is assigned
     */
    protected final String dataKey;

    /**
     * The name of the action, only used to provide a meaningful name via getName()
     */
    private final String actionName;

    /**
     * Reference to the class of the generic action.
     * This reference is held to prevent the {@link ClassInjector} from reusing this action.
     */
    private final InjectedClass<?> actionClass;

    /**
     * Reference to the actual action instance.
     * This corresponds to the value of {@link #actionClass} for {@link GenericActionTemplate#INSTANCE}.
     */
    protected final WeakReference<IGenericAction> action;


    protected BoundGenericAction(String dataKey, GenericActionConfig actionConfig, InjectedClass<?> actionClass) {
        this.dataKey = dataKey;
        actionName = actionConfig.getName();
        this.actionClass = actionClass;
        try {
            action = new WeakReference<>((IGenericAction) actionClass.getInjectedClassObject().get().getField("INSTANCE").get(null));
        } catch (Exception e) {
            throw new IllegalArgumentException("The given action is not based on the GenericActionTemplate");
        }
    }

    @Override
    public String getName() {
        return "Action '" + actionName + "' for '" + dataKey + "'";
    }

    /**
     * Binds a generic action to the given input argument values.
     *
     * @param dataKey             the data key under which the result of this action will be stored
     * @param actionConfig        the configuration of the used data action
     * @param action              the generated action class
     * @param constantAssignments a map mapping input variable names to their constant values
     * @param dynamicAssignments  a map mapping input variables to a function which is used to derive
     *                            the parameter value when the action is invoked
     * @return
     */
    public static BoundGenericAction bind(String dataKey,
                                          GenericActionConfig actionConfig,
                                          InjectedClass<?> action,
                                          Map<String, Object> constantAssignments,
                                          Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {

        if (dynamicAssignments.isEmpty()) {
            return new ConstantOnlyBoundGenericAction(dataKey, actionConfig, action, constantAssignments);
        } else {
            return new DynamicBoundGenericAction(dataKey, actionConfig, action, constantAssignments, dynamicAssignments);
        }
    }
}

class ConstantOnlyBoundGenericAction extends BoundGenericAction {

    private final Object[] arguments;

    public ConstantOnlyBoundGenericAction(String dataKey, GenericActionConfig actionConfig,
                                          InjectedClass<?> action, Map<String, Object> constantAssignments) {
        super(dataKey, actionConfig, action);

        // the additionalArgumentTypes is a sorted map
        // the order in which the arguments appear in this map correspond to the order in which their values
        // have to be placed in the arguments array
        arguments = actionConfig.getAdditionalArgumentTypes()
                .keySet().stream()
                .map(
                        constantAssignments::get
                ).toArray();
    }

    @Override
    public void execute(ExecutionContext context) {
        Object result = action.get().execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), arguments);
        context.getInspectitContext().setData(dataKey, result);
    }
}

class DynamicBoundGenericAction extends BoundGenericAction {

    /**
     * A template containing the already assigned constant arguments for this generic action.
     * As the same {@link DynamicBoundGenericAction} instance could potentially be used by multiple threads,
     * this array needs to be copied before the dynamicAssignments can be performed.
     */
    private final Object[] argumentsTemplate;

    /**
     * An array containing (a) the index of the addition input to assign and (b) a function for defining the value.
     * The index corresponds to the index of the parameter in {@link GenericActionConfig#getAdditionalArgumentTypes()}.
     * Therefore the index corresponds to the position in the additionalArgumetns array with which the
     * {@link IGenericAction#execute(Object[], Object, Object, Throwable, Object[])} function is called.
     */
    private Pair<Integer, Function<ExecutionContext, Object>>[] dynamicAssignments;

    public DynamicBoundGenericAction(String dataKey, GenericActionConfig actionConfig,
                                     InjectedClass<?> action, Map<String, Object> constantAssignments,
                                     Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {
        super(dataKey, actionConfig, action);

        // the sorted additionalArgumentTypes map defines the number and the order of the additional input
        // parameters the generic action expects
        // therefore we can already reserve the exact amount of space needed for the argumentsTemplate
        int numArgs = actionConfig.getAdditionalArgumentTypes().size();
        argumentsTemplate = new Object[numArgs];

        List<Pair<Integer, Function<ExecutionContext, Object>>> dynamicAssignmentsWithIndices = new ArrayList<>();

        //we now loop over the additionalArgumentTypes map and remember the index of the corresponding parameter
        //If the parameter is defined through a constant assignment we simply place it in the argumentsTemplate at the
        //index of the parameter.
        //if the parameter is defined through a dynamic assignment we cannot directly store the value already in the template.
        //Instead we remember the index and the function used to perform the assignment in dynamicAssignments.

        int idx = 0;
        for (String argName : actionConfig.getAdditionalArgumentTypes().keySet()) {
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

        Object result = action.get().execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), args);
        context.getInspectitContext().setData(dataKey, result);
    }
}

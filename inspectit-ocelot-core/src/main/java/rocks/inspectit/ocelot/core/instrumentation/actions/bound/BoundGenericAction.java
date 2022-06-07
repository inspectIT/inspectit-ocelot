package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import lombok.experimental.NonFinal;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.actions.template.GenericActionTemplate;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.hook.VariableAccessor;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Represents a {@link IGenericAction} which has values bound to its input arguments.
 * The bound values can be either constants or dynamically computed based on the {@link IHookAction.ExecutionContext}.
 */
@NonFinal
public abstract class BoundGenericAction implements IHookAction {

    /**
     * The key to store the action result in the data context.
     */
    private final String dataKey;

    /**
     * The action's name.
     */
    private final String name;

    /**
     * Whether this action is a void action and does not write any data to the context.
     */
    private final boolean voidAction;

    /**
     * Reference to the class of the generic action.
     * This reference is held to prevent the {@link ClassInjector} from reusing this action.
     */
    private final InjectedClass<?> actionClass;

    /**
     * Reference to the actual action instance.
     * This corresponds to the value of {@link #actionClass} for {@link GenericActionTemplate#INSTANCE}.
     * Note that because BoundGenericAction actions are only called from methods which reside inside the same class loader
     * as the reference IGenericAction, this WeakReference can never be null when {@link #execute(ExecutionContext)} is called.
     */
    protected final WeakReference<IGenericAction> action;

    /**
     * Binds a generic action to the given input argument values.
     *
     * @param dataKey             the data key under which the result of this action will be stored
     * @param actionConfig        the configuration of the used data action
     * @param action              the generated action class
     * @param constantAssignments a map mapping input variable names to their constant values
     * @param dynamicAssignments  a map mapping input variables to a function which is used to derive
     *                            the parameter value when the action is invoked
     */
    public static BoundGenericAction bind(String dataKey, GenericActionConfig actionConfig, InjectedClass<?> action, Map<String, Object> constantAssignments, Map<String, VariableAccessor> dynamicAssignments) {
        if (dynamicAssignments.isEmpty()) {
            return new ConstantOnlyBoundGenericAction(dataKey, actionConfig, action, constantAssignments);
        } else {
            return new DynamicBoundGenericAction(dataKey, actionConfig, action, constantAssignments, dynamicAssignments);
        }
    }

    protected BoundGenericAction(String dataKey, GenericActionConfig actionConfig, InjectedClass<?> actionClass) {
        this.dataKey = dataKey;
        this.name = actionConfig.getName();
        this.voidAction = actionConfig.isVoid();
        this.actionClass = actionClass;
        try {
            action = new WeakReference<>((IGenericAction) actionClass.getInjectedClassObject()
                    .get()
                    .getField("INSTANCE")
                    .get(null));
        } catch (Exception e) {
            throw new IllegalArgumentException("The given action is not based on the GenericActionTemplate");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Action '" + getName() + "' for data key '" + dataKey + "'";
    }

    @Override
    public void execute(ExecutionContext context) {
        Object[] actionArguments = getActionArguments(context);

        Object result = action.get()
                .execute(context.getMethodArguments(), context.getThiz(), context.getReturnValue(), context.getThrown(), actionArguments);

        if (!voidAction) {
            context.getInspectitContext().setData(dataKey, result);
        }
    }

    /**
     * Returns the arguments used by the action. These are the input arguments specified in the action definition (configuration).
     *
     * @param context the context to use to derive dynamic arguments (aka. data inputs).
     *
     * @return the sorted array of action arguments
     */
    protected abstract Object[] getActionArguments(ExecutionContext context);
}



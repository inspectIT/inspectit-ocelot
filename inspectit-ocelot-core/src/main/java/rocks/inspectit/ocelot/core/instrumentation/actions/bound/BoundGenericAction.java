package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import lombok.experimental.NonFinal;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.actions.template.GenericActionTemplate;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.injection.ClassInjector;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a {@link IGenericAction} which has values bound to its input arguments.
 * The bound values can be either constants or dynamically computed based on the {@link IHookAction.ExecutionContext}.
 */
@NonFinal
public abstract class BoundGenericAction implements IHookAction {

    /**
     * The name of the call, usually equal to the data key.
     */
    private final String callName;

    /**
     * The name of the action, only used to provide a meaningful name via getName()
     */
    protected final String actionName;

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


    protected BoundGenericAction(String callName, GenericActionConfig actionConfig, InjectedClass<?> actionClass) {
        actionName = actionConfig.getName();
        this.callName = callName;
        this.actionClass = actionClass;
        try {
            action = new WeakReference<>((IGenericAction) actionClass.getInjectedClassObject().get().getField("INSTANCE").get(null));
        } catch (Exception e) {
            throw new IllegalArgumentException("The given action is not based on the GenericActionTemplate");
        }
    }

    @Override
    public String getName() {
        return "Action '" + actionName + "' for call '" + callName + "'";
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
            if (actionConfig.isVoid()) {
                return new VoidConstantOnlyBoundGenericAction(dataKey, actionConfig, action, constantAssignments);
            } else {
                return new NonVoidConstantOnlyBoundGenericAction(dataKey, dataKey, actionConfig, action, constantAssignments);
            }
        } else {
            if (actionConfig.isVoid()) {
                return new VoidDynamicBoundGenericAction(dataKey, actionConfig, action, constantAssignments, dynamicAssignments);
            } else {
                return new NonVoidDynamicBoundGenericAction(dataKey, dataKey, actionConfig, action, constantAssignments, dynamicAssignments);
            }
        }
    }
}



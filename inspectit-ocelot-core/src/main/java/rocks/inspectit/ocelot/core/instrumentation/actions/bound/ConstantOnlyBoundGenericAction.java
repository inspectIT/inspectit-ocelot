package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.Map;

/**
 * Base class for {@link BoundGenericAction}s which only have
 * constant values for their input parameters assigned.
 */
public class ConstantOnlyBoundGenericAction extends BoundGenericAction {

    private final Object[] arguments;

    public ConstantOnlyBoundGenericAction(String dataKey, GenericActionConfig actionConfig, InjectedClass<?> action, Map<String, Object> constantAssignments) {
        super(dataKey, actionConfig, action);

        // the additionalArgumentTypes is a sorted map
        // the order in which the arguments appear in this map correspond to the order in which their values
        // have to be placed in the arguments array
        arguments = actionConfig.getAdditionalArgumentTypes().keySet().stream().map(constantAssignments::get).toArray();
    }

    @Override
    protected Object[] getActionArguments(ExecutionContext context) {
        return arguments;
    }
}

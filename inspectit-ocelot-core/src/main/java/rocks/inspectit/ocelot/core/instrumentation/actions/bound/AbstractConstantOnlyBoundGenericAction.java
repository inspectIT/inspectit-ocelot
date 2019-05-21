package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.Map;

/**
 * Base class for {@link BoundGenericAction}s which only have
 * constant values for their input parameters assigned.
 */
abstract class AbstractConstantOnlyBoundGenericAction extends BoundGenericAction {

    protected final Object[] arguments;

    public AbstractConstantOnlyBoundGenericAction(String callName, GenericActionConfig actionConfig,
                                                  InjectedClass<?> action, Map<String, Object> constantAssignments) {
        super(callName, actionConfig, action);

        // the additionalArgumentTypes is a sorted map
        // the order in which the arguments appear in this map correspond to the order in which their values
        // have to be placed in the arguments array
        arguments = actionConfig.getAdditionalArgumentTypes()
                .keySet().stream()
                .map(
                        constantAssignments::get
                ).toArray();
    }
}

package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.Map;
import java.util.function.Function;


/**
 * Variant of a {@link AbstractDynamicBoundGenericAction} which does
 * not write the value returned by the invoked action to the context,
 * as it is a void action.
 */
class VoidDynamicBoundGenericAction extends AbstractDynamicBoundGenericAction {


    VoidDynamicBoundGenericAction(String callName, GenericActionConfig actionConfig,
                                  InjectedClass<?> action, Map<String, Object> constantAssignments,
                                  Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {
        super(callName, actionConfig, action, constantAssignments, dynamicAssignments);
    }

    @Override
    public void execute(ExecutionContext context) {
        Object[] args = buildAdditionalArguments(context);
        action.get().execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), args);
    }
}

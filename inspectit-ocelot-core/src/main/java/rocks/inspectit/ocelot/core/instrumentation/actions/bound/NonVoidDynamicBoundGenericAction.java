package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.Map;
import java.util.function.Function;


/**
 * Variant of a {@link AbstractDynamicBoundGenericAction} which returns
 * the value computed by the invoked action.
 */
class NonVoidDynamicBoundGenericAction extends AbstractDynamicBoundGenericAction {

    private final String dataKey;

    NonVoidDynamicBoundGenericAction(String callName, String dataKey, GenericActionConfig actionConfig,
                                     InjectedClass<?> action, Map<String, Object> constantAssignments,
                                     Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {
        super(callName, actionConfig, action, constantAssignments, dynamicAssignments);
        this.dataKey = dataKey;
    }

    @Override
    public void execute(ExecutionContext context) {
        Object[] args = buildAdditionalArguments(context);
        Object result = action.get().execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), args);
        context.getInspectitContext().setData(dataKey, result);
    }
}

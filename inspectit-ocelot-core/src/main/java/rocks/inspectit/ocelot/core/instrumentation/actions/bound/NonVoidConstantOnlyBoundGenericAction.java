package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.Map;

/**
 * Variant of a {@link AbstractConstantOnlyBoundGenericAction} which returns
 * the value computed by the invoked action.
 */
class NonVoidConstantOnlyBoundGenericAction extends AbstractConstantOnlyBoundGenericAction {

    private final String dataKey;

    NonVoidConstantOnlyBoundGenericAction(String dataKey, String callName, GenericActionConfig actionConfig,
                                          InjectedClass<?> action, Map<String, Object> constantAssignments) {
        super(callName, actionConfig, action, constantAssignments);
        this.dataKey = dataKey;
    }

    @Override
    public void execute(ExecutionContext context) {
        Object result = action.get().execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), arguments);
        context.getInspectitContext().setData(dataKey, result);
    }
}

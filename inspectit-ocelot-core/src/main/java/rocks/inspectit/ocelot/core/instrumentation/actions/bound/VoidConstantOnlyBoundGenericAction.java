package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.Map;

class VoidConstantOnlyBoundGenericAction extends AbstractConstantOnlyBoundGenericAction {

    VoidConstantOnlyBoundGenericAction(String callName, GenericActionConfig actionConfig,
                                       InjectedClass<?> action, Map<String, Object> constantAssignments) {
        super(callName, actionConfig, action, constantAssignments);
    }

    @Override
    public void execute(ExecutionContext context) {
        action.get().execute(context.getMethodArguments(), context.getThiz(),
                context.getReturnValue(), context.getThrown(), arguments);
    }
}

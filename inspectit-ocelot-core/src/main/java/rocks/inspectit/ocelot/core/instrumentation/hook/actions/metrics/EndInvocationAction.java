package rocks.inspectit.ocelot.core.instrumentation.hook.actions.metrics;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.metrics.concurrent.ConcurrentInvocationManager;

/**
 * Action to end a concurrent invocation with {@link ConcurrentInvocationManager}
 */
@Value
public class EndInvocationAction implements IHookAction {

    /**
     * The custom name of the operation, whose invocation will be recorded.
     */
    String operation;

    /**
     * The manager to record concurrent invocations
     */
    ConcurrentInvocationManager concurrentInvocationManager;

    @Override
    public void execute(ExecutionContext context) {
        concurrentInvocationManager.removeInvocation(operation);
    }

    @Override
    public String getName() {
        return "End concurrent invocation";
    }
}

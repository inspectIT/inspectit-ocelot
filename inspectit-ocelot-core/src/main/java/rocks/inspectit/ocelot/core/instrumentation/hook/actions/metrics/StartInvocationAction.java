package rocks.inspectit.ocelot.core.instrumentation.hook.actions.metrics;

import lombok.Value;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.metrics.concurrent.ConcurrentInvocationManager;

/**
 * Action to start a concurrent invocation with {@link ConcurrentInvocationManager}
 */
@Value
public class StartInvocationAction implements IHookAction {

    /**
     * The name of the operation, whose invocation will be recorded.
     */
    String operation;

    /**
     * The manager to record concurrent invocations
     */
    ConcurrentInvocationManager concurrentInvocationManager;

    @Override
    public void execute(ExecutionContext context) {
        concurrentInvocationManager.addInvocation(operation);
    }

    @Override
    public String getName() {
        return "Start concurrent invocation";
    }
}

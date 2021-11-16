package rocks.inspectit.ocelot.core.selfmonitoring;

import io.opencensus.common.Scope;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * Scope for an {@link IHookAction} executed in the {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook}
 */
public interface IActionScope extends Scope {

    /**
     * Starts the scope.
     */
    public abstract void start();

    /**
     * Starts the scope and stores the given start time of the action
     *
     * @param startTimeNanos The start time of the action in nanoseconds.
     */
    public abstract void start(long startTimeNanos);

}


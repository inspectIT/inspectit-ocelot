package rocks.inspectit.ocelot.core.selfmonitoring;

import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * Scope for an {@link IHookAction} executed in the {@link rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook}
 */
public interface IActionScope extends AutoCloseable {

    IActionScope NOOP_ACTION_SCOPE = () -> {
    };
}


package rocks.inspectit.ocelot.core.instrumentation.hook;

import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;

/**
 * A function for reading a variable from the {@link rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction.ExecutionContext}.
 * If the variable starts with an underscore, it is a special variable.
 * For example a {@link VariableAccessor} for "_arg3" is guaranteed to return the method call argument with index 3 in the context of the current method call.
 * If the variable does not begin with an underscore, it is fetched from the {@link rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext}.
 */
@FunctionalInterface
public interface VariableAccessor {

    /**
     * Reads a fixed variable.
     * The variable can be a special variable as well as fetched from the {@link rocks.inspectit.ocelot.bootstrap.exposed.InspectitContext}.
     *
     * @param context the context within the variable is queried
     *
     * @return the value of the variable
     */
    Object get(IHookAction.ExecutionContext context);
}

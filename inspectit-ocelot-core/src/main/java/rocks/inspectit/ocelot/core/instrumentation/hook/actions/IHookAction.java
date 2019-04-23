package rocks.inspectit.ocelot.core.instrumentation.hook.actions;

import lombok.Value;
import lombok.experimental.NonFinal;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContext;
import rocks.inspectit.ocelot.core.instrumentation.hook.MethodHook;

/**
 * Interface used to define any kind of action which is executed in a {@link MethodHook} on method enter or exit.
 * Possible actions are for example data provider executions or the metrics collection.
 */
public interface IHookAction {

    /**
     * Executes this action.
     * If this method throws an exception the provider will be disabled for the containing method hook until it is reconfigured.
     *
     * @param context the context information which can be used for the execution
     */
    void execute(ExecutionContext context);

    /**
     * A logical name used to print meaningful log messages.
     *
     * @return the logical name
     */
    String getName();

    /**
     * Simple container object storing all possible context information which may be
     * accessed by the hook action.
     */
    @Value
    @NonFinal
    class ExecutionContext {

        /**
         * The arguments passed to the instrumented method
         */
        private Object[] methodArguments;

        /**
         * If available stores the "this" reference for which the instrumented method was executed.
         * If not available, e.g. because the method is static or the hook is executed on constructor entry, this will be null.
         */
        private Object thiz;

        /**
         * If available stores the value returned by the instrumented method, null otherwise.
         */
        private Object returnValue;

        /**
         * If the instrumented method threw an exception, it is stored in this object.
         * Null otherwise.
         */
        private Throwable thrown;

        /**
         * The hook to which the executed hook actions belongs.
         */
        private MethodHook hook;

        /**
         * The context to store and read data.
         */
        private InspectitContext inspectitContext;

    }
}

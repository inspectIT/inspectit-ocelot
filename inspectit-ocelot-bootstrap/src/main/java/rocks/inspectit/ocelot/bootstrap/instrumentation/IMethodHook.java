package rocks.inspectit.ocelot.bootstrap.instrumentation;

import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;

public interface IMethodHook {

    /**
     * Called when the hooked method is entered.
     *
     * @param instrumentedMethodArgs the arguments passed to the method for which the hook is executed
     * @param thiz                   the "this" instance of the invoked method, null if the invoked method is static
     *
     * @return the opened context, will be passed to onExit
     */
    InternalInspectitContext onEnter(Object[] instrumentedMethodArgs, Object thiz);

    /**
     * Called when the hooked method exits.
     *
     * @param instrumentedMethodArgs the arguments passed to the method for which the hook is executed
     * @param thiz                   the "this" instance of the invoked method, null if the invoked method is static
     * @param returnValue            the return value returned by the target method, if this hook is executed at the end and no exception was thrown
     * @param thrown                 the exception thrown by the instrumented method, null otherwise
     * @param context                the context returned by the onEnter call
     */
    void onExit(Object[] instrumentedMethodArgs, Object thiz, Object returnValue, Throwable thrown, InternalInspectitContext context);
}

package rocks.inspectit.oce.core.instrumentation.hook;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rocks.inspectit.oce.bootstrap.context.IInspectitContext;
import rocks.inspectit.oce.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.context.ContextManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Each {@link MethodHook} instances defines for a single method which actions are performed.
 * This defines for example which data providers are executed or which emtrics are collected.
 * {@link MethodHook}s are created, destroyed and mapped to methods via the {@link HookManager}.
 */
@Slf4j
@Data
@Builder
public class MethodHook implements IMethodHook {

    /**
     * The configuraiton on which this hook is based.
     * This object can be compared against newly derived configurations to see if the hook requires an update.
     */
    private final MethodHookConfiguration sourceConfiguration;

    /**
     * The context manager used to create the inspectit context.
     */
    private final ContextManager inspectitContextManager;

    /**
     * The list of actions to execute when the instrumented method is entered.
     */
    @Builder.Default
    private CopyOnWriteArrayList<IHookAction> entryActions = new CopyOnWriteArrayList<>();

    /**
     * The list of actions to execute when the instrumented method is exited.
     */
    @Builder.Default
    private CopyOnWriteArrayList<IHookAction> exitActions = new CopyOnWriteArrayList<>();

    /**
     * A symbolic identifier to identify the instrumented method.
     * Purely used to print meaningful log messages.
     */
    private final String methodName;

    private final WeakReference<Method> hookedMethod;
    private final WeakReference<Constructor<?>> hookedConstructor;
    private final WeakReference<Class<?>> hookedClass;


    @Override
    public IInspectitContext onEnter(Object[] args, Object thiz) {
        val inspectitContext = inspectitContextManager.enterNewContext();
        val executionContext = new IHookAction.ExecutionContext(args, thiz, null, null, this, inspectitContext);

        for (val action : entryActions) {
            try {
                action.execute(executionContext);
            } catch (Throwable t) {
                log.error("Entry action {} executed for method {} threw an exception and from now on is disabled!", action.getName(), methodName, t);
                entryActions.remove(action);
            }
        }

        inspectitContext.makeActive();
        return inspectitContext;
    }

    @Override
    public void onExit(Object[] args, Object thiz, Object returnValue, Throwable thrown, IInspectitContext context) {
        val executionContext = new IHookAction.ExecutionContext(args, thiz, returnValue, thrown, this, context);
        for (val action : exitActions) {
            try {
                action.execute(executionContext);
            } catch (Throwable t) {
                log.error("Exit action {} executed for method {} threw an exception and from now on is disabled!", action.getName(), methodName, t);
                exitActions.remove(action);
            }
        }
        context.close();
    }

    /**
     * If this hook is applied on a method, this getter returns the reflection access to it.
     * Otherwise null.
     *
     * @return the instrumented method or null
     */
    public Method getHookedMethod() {
        return hookedMethod == null ? null : hookedMethod.get();
    }


    /**
     * If this hook is applied on a constructor, this getter returns the reflection access to it.
     * Otherwise null.
     *
     * @return the instrumented constructor or null
     */
    public Constructor<?> getHookedConstructor() {
        return hookedConstructor == null ? null : hookedConstructor.get();
    }

    /**
     * @return The class on which this method hook is applied, never null as long as the class has not been garbage collected.
     */
    public Class<?> getHookedClass() {
        return hookedClass == null ? null : hookedClass.get();
    }

}

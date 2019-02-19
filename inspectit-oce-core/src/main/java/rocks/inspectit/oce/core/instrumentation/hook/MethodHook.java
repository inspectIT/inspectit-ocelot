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

@Slf4j
@Data
@Builder
public class MethodHook implements IMethodHook {

    private final MethodHookConfiguration sourceConfiguration;

    private final ContextManager inspectitContextManager;

    @Builder.Default
    private CopyOnWriteArrayList<IHookAction> entryActions = new CopyOnWriteArrayList<>();

    @Builder.Default
    private CopyOnWriteArrayList<IHookAction> exitActions = new CopyOnWriteArrayList<>();

    private final String methodSignature;

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
                log.error("Entry action {} executed for method {} threw an exception and from now on is disabled!", action.getName(), methodSignature, t);
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
                log.error("Exit action {} executed for method {} threw an exception and from now on is disabled!", action.getName(), methodSignature, t);
                exitActions.remove(action);
            }
        }
        context.close();
    }
}

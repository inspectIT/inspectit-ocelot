package rocks.inspectit.ocelot.core.instrumentation.hook;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.bootstrap.context.InternalInspectitContext;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IMethodHook;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.context.InspectitContextImpl;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionMetricsRecorder;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;
import rocks.inspectit.ocelot.core.selfmonitoring.IActionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Each {@link MethodHook} instances defines for a single method which actions are performed.
 * This defines for example which generic actions are executed or which metrics are collected.
 * {@link MethodHook}s are created, destroyed and mapped to methods via the {@link HookManager}.
 */
@Slf4j
@Value
public class MethodHook implements IMethodHook {

    /**
     * The configuration on which this hook is based.
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
    private final List<IHookAction> entryActions;

    /**
     * The list of actions to execute when the instrumented method is exited.
     */
    private final List<IHookAction> exitActions;

    /**
     * The subset of {@link #entryActions}, which are actually active.
     * Initially, this list contains the same elements as {@link #entryActions}.
     */
    private final CopyOnWriteArrayList<IHookAction> activeEntryActions;

    /**
     * The subset of {@link #exitActions}, which are actually active.
     * Initially, this list contains the same elements as {@link #exitActions}.
     */
    private final CopyOnWriteArrayList<IHookAction> activeExitActions;

    /**
     * Stores details regarding the hooked method
     */
    private final MethodReflectionInformation methodInformation;

    /**
     * The metrics recorder for the {@link IHookAction}
     */
    private final ActionMetricsRecorder actionMetricsRecorder;

    /**
     * The factory that creates/spawns new scopes for an {@link IHookAction}
     */
    private final ActionScopeFactory actionScopeFactory;

    @Builder
    public MethodHook(MethodHookConfiguration sourceConfiguration, ContextManager inspectitContextManager, @Singular List<IHookAction> entryActions, @Singular List<IHookAction> exitActions, MethodReflectionInformation methodInformation, ActionMetricsRecorder actionMetricsRecorder, ActionScopeFactory actionScopeFactory) {
        this.sourceConfiguration = sourceConfiguration;
        this.inspectitContextManager = inspectitContextManager;
        this.entryActions = new ArrayList<>(entryActions);
        activeEntryActions = new CopyOnWriteArrayList<>(entryActions);
        this.exitActions = new ArrayList<>(exitActions);
        activeExitActions = new CopyOnWriteArrayList<>(exitActions);
        this.methodInformation = methodInformation;
        this.actionMetricsRecorder = actionMetricsRecorder;
        this.actionScopeFactory = actionScopeFactory;
    }

    @Override
    public InternalInspectitContext onEnter(Object[] args, Object thiz) {
        InspectitContextImpl inspectitContext = inspectitContextManager.enterNewContext();
        IHookAction.ExecutionContext executionContext = new IHookAction.ExecutionContext(args, thiz, null, null, this, inspectitContext);

        for (IHookAction action : activeEntryActions) {
            try (IActionScope scope = actionScopeFactory.getScope(action)) {
                action.execute(executionContext);
            } catch (Throwable t) {
                log.error("Entry action {} executed for method {} threw an exception and from now on is disabled!", action.getName(), methodInformation.getMethodFQN(), t);
                activeEntryActions.remove(action);
            }
        }

        inspectitContext.makeActive();
        return inspectitContext;
    }

    @Override
    public void onExit(Object[] args, Object thiz, Object returnValue, Throwable thrown, InternalInspectitContext context) {
        IHookAction.ExecutionContext executionContext = new IHookAction.ExecutionContext(args, thiz, returnValue, thrown, this, (InspectitContextImpl) context);
        for (IHookAction action : activeExitActions) {
            try (IActionScope scope = actionScopeFactory.getScope(action)) {
                action.execute(executionContext);
            } catch (Throwable t) {
                log.error("Exit action {} executed for method {} threw an exception and from now on is disabled!", action.getName(), methodInformation.getMethodFQN(), t);
                activeExitActions.remove(action);
            }
        }
        context.close();
    }

    /**
     * @return An exact copy of this method hook but with all deactivated actions reactivated.
     */
    public MethodHook getResettedCopy() {
        return new MethodHook(sourceConfiguration, inspectitContextManager, entryActions, exitActions, methodInformation, actionMetricsRecorder, actionScopeFactory);
    }

}

package rocks.inspectit.ocelot.core.instrumentation.hook;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
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
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.TracingHookAction;
import rocks.inspectit.ocelot.core.selfmonitoring.ActionScopeFactory;
import rocks.inspectit.ocelot.core.selfmonitoring.IActionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Each {@link MethodHook} instances defines for a single method which actions are performed.
 * This defines for example which generic actions are executed or which metrics are collected.
 * {@link MethodHook}s are created, destroyed and mapped to methods via the {@link HookManager}.
 */
@Slf4j
@Value
public class MethodHook implements IMethodHook {

    private static final String SPAN_ATTRIBUTE_PREFIX = "inspectit.debug.method-hook.";

    private static final AttributeValue NULL_STRING_ATTRIBUTE = AttributeValue.stringAttributeValue("<NULL>");

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
     * The factory that creates/spawns new scopes for an {@link IHookAction}
     */
    private final ActionScopeFactory actionScopeFactory;

    @Builder
    public MethodHook(MethodHookConfiguration sourceConfiguration, ContextManager inspectitContextManager, @Singular List<IHookAction> entryActions, @Singular List<IHookAction> exitActions, MethodReflectionInformation methodInformation, ActionScopeFactory actionScopeFactory) {
        this.sourceConfiguration = sourceConfiguration;
        this.inspectitContextManager = inspectitContextManager;
        this.entryActions = new ArrayList<>(entryActions);
        activeEntryActions = new CopyOnWriteArrayList<>(entryActions);
        this.exitActions = new ArrayList<>(exitActions);
        activeExitActions = new CopyOnWriteArrayList<>(exitActions);
        this.methodInformation = methodInformation;
        if (actionScopeFactory == null) {
            throw new IllegalArgumentException("ActionScopeFactory must not be null!");
        }
        this.actionScopeFactory = actionScopeFactory;
    }

    @Override
    public InternalInspectitContext onEnter(Object[] args, Object thiz) {
        Span hookSpan = null;
        try {
            // flags the thread that it is now in action execution
            HookManager.RECURSION_GATE.set(true);

            InspectitContextImpl inspectitContext = inspectitContextManager.enterNewContext();

            hookSpan = getEntryHookTracingSpan();
            recordContextDataInSpan(hookSpan, inspectitContext, "before.");

            IHookAction.ExecutionContext executionContext = new IHookAction.ExecutionContext(args, thiz, null, null, this, inspectitContext, hookSpan);

            for (IHookAction action : activeEntryActions) {
                try (IActionScope scope = actionScopeFactory.createScope(action)) {
                    action.execute(executionContext);
                } catch (Throwable t) {
                    log.error("Entry action {} executed for method {} threw an exception and from now on is disabled!", action, methodInformation
                            .getMethodFQN(), t);
                    activeEntryActions.remove(action);
                }
            }

            recordContextDataInSpan(hookSpan, inspectitContext, "after.");

            inspectitContext.makeActive();
            return inspectitContext;
        } finally {
            if (hookSpan != null) {
                hookSpan.end();
            }

            HookManager.RECURSION_GATE.set(false);
        }
    }

    @Override
    public void onExit(Object[] args, Object thiz, Object returnValue, Throwable thrown, InternalInspectitContext context) {
        Span hookSpan = null;
        try {
            // flags the thread that it is now in action execution
            HookManager.RECURSION_GATE.set(true);

            hookSpan = getExitHookTracingSpan();
            recordContextDataInSpan(hookSpan, context, "before.");

            IHookAction.ExecutionContext executionContext = new IHookAction.ExecutionContext(args, thiz, returnValue, thrown, this, (InspectitContextImpl) context, hookSpan);

            for (IHookAction action : activeExitActions) {
                try (IActionScope scope = actionScopeFactory.createScope(action)) {
                    action.execute(executionContext);
                } catch (Throwable t) {
                    log.error("Exit action {} executed for method {} threw an exception and from now on is disabled!", action, methodInformation
                            .getMethodFQN(), t);
                    activeExitActions.remove(action);
                }
            }

            recordContextDataInSpan(hookSpan, context, "after.");

            context.close();
        } finally {
            if (hookSpan != null) {
                hookSpan.end();
            }

            HookManager.RECURSION_GATE.set(false);
        }
    }

    /**
     * @return An exact copy of this method hook but with all deactivated actions reactivated.
     */
    public MethodHook getResettedCopy() {
        return new MethodHook(sourceConfiguration, inspectitContextManager, entryActions, exitActions, methodInformation, actionScopeFactory);
    }

    private Span getEntryHookTracingSpan() {
        if (sourceConfiguration.isTraceEntryHook()) {
            return getHookTracingSpan("entryHook");
        } else {
            return null;
        }
    }

    private Span getExitHookTracingSpan() {
        if (sourceConfiguration.isTraceExitHook()) {
            return getHookTracingSpan("exitHook");
        } else {
            return null;
        }
    }

    private Span getHookTracingSpan(String hookName) {
        return Tracing.getTracer()
                .spanBuilder(TracingHookAction.DEBUG_SPAN_NAME_PREFIX + hookName + "<" + methodInformation.getMethodFQN() + ">")
                .startSpan();
    }

    private void recordContextDataInSpan(Span span, InternalInspectitContext context, String prefix) {
        if (span != null) {
            Iterable<Map.Entry<String, Object>> contextData = context.getData();
            for (Map.Entry<String, Object> entry : contextData) {
                recordAttribute(span, "context." + prefix + entry.getKey(), entry.getValue());
            }
        }
    }

    private void recordAttribute(Span span, String name, Object value) {
        span.putAttribute(SPAN_ATTRIBUTE_PREFIX + name, value != null ? AttributeValue.stringAttributeValue(value.toString()) : NULL_STRING_ATTRIBUTE);
    }
}

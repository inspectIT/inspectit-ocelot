package rocks.inspectit.ocelot.core.instrumentation.hook;

import io.opencensus.stats.StatsRecorder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.context.ContextManager;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.ConditionalHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.IHookAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.MetricsRecorder;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.ContinueOrStartSpanAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.EndSpanAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.StoreSpanAction;
import rocks.inspectit.ocelot.core.instrumentation.hook.actions.span.WriteSpanAttributesAction;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * This class is responsible for translating {@link MethodHookConfiguration}s
 * into executable {@link MethodHook}s.
 */
@Component
@Slf4j
public class MethodHookGenerator {

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private MeasuresAndViewsManager metricsManager;

    @Autowired
    private StatsRecorder statsRecorder;

    @Autowired
    private ActionCallGenerator actionCallGenerator;

    /**
     * Builds a executable method hook based on the given configuration.
     *
     * @param declaringClass teh class defining the method which is being hooked
     * @param method         a method descriptor of the hooked method
     * @param config         the configuration to use for building the hook
     * @return the generated method hook
     */
    public MethodHook buildHook(Class<?> declaringClass, MethodDescription method, MethodHookConfiguration config) {
        val builder = MethodHook.builder()
                .inspectitContextManager(contextManager)
                .sourceConfiguration(config);

        val methodInfo = MethodReflectionInformation.createFor(declaringClass, method);
        builder.methodInformation(methodInfo);

        val entryActions = new CopyOnWriteArrayList<IHookAction>();
        entryActions.addAll(buildActionCalls(config.getEntryActions(), methodInfo));
        entryActions.addAll(buildTracingEntryActions(config.getTracing()));
        builder.entryActions(entryActions);

        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        exitActions.addAll(buildActionCalls(config.getExitActions(), methodInfo));
        exitActions.addAll(buildTracingExitActions(config.getTracing()));
        buildMetricsRecorder(config)
                .ifPresent(exitActions::add);
        builder.exitActions(exitActions);

        return builder.build();
    }


    private List<IHookAction> buildTracingEntryActions(RuleTracingSettings tracing) {
        if (tracing.isStartSpan() || tracing.getContinueSpan() != null) {

            Predicate<IHookAction.ExecutionContext> startCondition = (ctx) -> false;
            if (tracing.isStartSpan()) {
                startCondition = ConditionalHookAction.getAsPredicate(tracing.getStartSpanConditions());
            }

            Predicate<IHookAction.ExecutionContext> continueCondition = (ctx) -> false;
            if (tracing.getContinueSpan() != null) {
                continueCondition = ConditionalHookAction.getAsPredicate(tracing.getContinueSpanConditions());
            }

            IHookAction beginTraceAction = new ContinueOrStartSpanAction(tracing.getName(), tracing.getKind(),
                    tracing.getContinueSpan(), continueCondition, startCondition);

            val result = new ArrayList<IHookAction>();
            result.add(beginTraceAction);

            if (tracing.getStoreSpan() != null) {
                result.add(new StoreSpanAction(tracing.getStoreSpan()));
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private List<IHookAction> buildTracingExitActions(RuleTracingSettings tracing) {
        val result = new ArrayList<IHookAction>();

        val attributes = tracing.getAttributes();
        if (!attributes.isEmpty()) {
            IHookAction endTraceAction = new WriteSpanAttributesAction(attributes);
            IHookAction actionWithConditions = ConditionalHookAction.wrapWithConditionChecks(tracing.getAttributeConditions(), endTraceAction);
            result.add(actionWithConditions);
        }

        if (tracing.isEndSpan() && (tracing.isStartSpan() || tracing.getContinueSpan() != null)) {
            val endSpanAction = new EndSpanAction(ConditionalHookAction.getAsPredicate(tracing.getEndSpanConditions()));
            result.add(endSpanAction);
        }
        return result;
    }

    private Optional<IHookAction> buildMetricsRecorder(MethodHookConfiguration config) {
        if (!config.getConstantMetrics().isEmpty() || !config.getDataMetrics().isEmpty()) {
            val recorder = new MetricsRecorder(config.getConstantMetrics(), config.getDataMetrics(), metricsManager, statsRecorder);
            return Optional.of(recorder);
        } else {
            return Optional.empty();
        }
    }

    private List<IHookAction> buildActionCalls(List<ActionCallConfig> calls, MethodReflectionInformation methodInfo) {

        List<IHookAction> result = new ArrayList<>();
        for (val call : calls) {
            try {
                result.add(actionCallGenerator.generateAndBindGenericAction(methodInfo, call));
            } catch (Exception e) {
                log.error("Failed to build action {} for data {} on method {}, no value will be assigned",
                        call.getAction().getName(), call.getName(), methodInfo.getMethodFQN(), e);
            }
        }
        return result;
    }


}

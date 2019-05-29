package rocks.inspectit.ocelot.core.instrumentation.hook;

import io.opencensus.stats.StatsRecorder;
import io.opencensus.trace.samplers.Samplers;
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

        RuleTracingSettings tracingSettings = config.getTracing();

        val entryActions = new CopyOnWriteArrayList<IHookAction>();
        entryActions.addAll(buildActionCalls(config.getEntryActions(), methodInfo));
        if (tracingSettings != null) {
            entryActions.addAll(buildTracingEntryActions(tracingSettings));
        }
        builder.entryActions(entryActions);

        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        exitActions.addAll(buildActionCalls(config.getExitActions(), methodInfo));
        if (tracingSettings != null) {
            exitActions.addAll(buildTracingExitActions(tracingSettings));
        }
        buildMetricsRecorder(config)
                .ifPresent(exitActions::add);
        builder.exitActions(exitActions);

        return builder.build();
    }


    private List<IHookAction> buildTracingEntryActions(RuleTracingSettings tracing) {
        if (tracing.getStartSpan() || tracing.getContinueSpan() != null) {

            val actionBuilder = ContinueOrStartSpanAction.builder();


            if (tracing.getStartSpan()) {
                actionBuilder
                        .startSpanCondition(ConditionalHookAction.getAsPredicate(tracing.getStartSpanConditions()))
                        .nameDataKey(tracing.getName())
                        .spanKind(tracing.getKind());
                configureSampling(tracing, actionBuilder);
            } else {
                actionBuilder.startSpanCondition(ctx -> false);
            }

            if (tracing.getContinueSpan() != null) {
                actionBuilder
                        .continueSpanCondition(ConditionalHookAction.getAsPredicate(tracing.getContinueSpanConditions()))
                        .continueSpanDataKey(tracing.getContinueSpan());
            } else {
                actionBuilder.continueSpanCondition(ctx -> false);
            }

            val result = new ArrayList<IHookAction>();
            result.add(actionBuilder.build());

            if (tracing.getStoreSpan() != null) {
                result.add(new StoreSpanAction(tracing.getStoreSpan()));
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private void configureSampling(RuleTracingSettings tracing, ContinueOrStartSpanAction.ContinueOrStartSpanActionBuilder actionBuilder) {
        try {
            double fixedProbability = Double.parseDouble(tracing.getSampleProbability());
            if (fixedProbability <= 0) {
                actionBuilder.staticSampler(Samplers.neverSample());
            } else if (fixedProbability >= 1) {
                actionBuilder.staticSampler(Samplers.alwaysSample());
            } else {
                actionBuilder.staticSampler(Samplers.probabilitySampler(fixedProbability));
            }
        } catch (NumberFormatException e) {
            actionBuilder.dynamicSampleProbabilityKey(tracing.getSampleProbability());
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

        if (tracing.getEndSpan() && (tracing.getStartSpan() || tracing.getContinueSpan() != null)) {
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

package rocks.inspectit.ocelot.core.instrumentation.hook;

import io.opencensus.stats.StatsRecorder;
import io.opencensus.trace.Sampler;
import io.opencensus.trace.samplers.Samplers;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.apache.commons.lang3.StringUtils;
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

import java.util.*;
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

    @Autowired
    private VariableAccessorFactory variableAccessorFactory;

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
        entryActions.addAll(buildActionCalls(config.getPreEntryActions(), methodInfo));
        entryActions.addAll(buildActionCalls(config.getEntryActions(), methodInfo));
        if (tracingSettings != null) {
            entryActions.addAll(buildTracingEntryActions(tracingSettings));
        }
        entryActions.addAll(buildActionCalls(config.getPostEntryActions(), methodInfo));
        builder.entryActions(entryActions);

        val exitActions = new CopyOnWriteArrayList<IHookAction>();
        exitActions.addAll(buildActionCalls(config.getPreExitActions(), methodInfo));
        exitActions.addAll(buildActionCalls(config.getExitActions(), methodInfo));
        if (tracingSettings != null) {
            exitActions.addAll(buildTracingExitActions(tracingSettings));
        }
        buildMetricsRecorder(config)
                .ifPresent(exitActions::add);
        exitActions.addAll(buildActionCalls(config.getPostExitActions(), methodInfo));
        builder.exitActions(exitActions);

        return builder.build();
    }


    private List<IHookAction> buildTracingEntryActions(RuleTracingSettings tracing) {
        if (tracing.getStartSpan() || tracing.getContinueSpan() != null) {

            val actionBuilder = ContinueOrStartSpanAction.builder();


            if (tracing.getStartSpan()) {
                VariableAccessor name = Optional.ofNullable(tracing.getName())
                        .map(variableAccessorFactory::getVariableAccessor).orElse(null);
                actionBuilder
                        .startSpanCondition(ConditionalHookAction.getAsPredicate(tracing.getStartSpanConditions(), variableAccessorFactory))
                        .nameAccessor(name)
                        .spanKind(tracing.getKind());
                configureSampling(tracing, actionBuilder);
            } else {
                actionBuilder.startSpanCondition(ctx -> false);
            }

            if (tracing.getContinueSpan() != null) {
                actionBuilder
                        .continueSpanCondition(ConditionalHookAction.getAsPredicate(tracing.getContinueSpanConditions(), variableAccessorFactory))
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
        String sampleProbability = tracing.getSampleProbability();
        if (!StringUtils.isBlank(sampleProbability)) {
            try {
                double constantProbability = Double.parseDouble(sampleProbability);
                Sampler sampler = Samplers.probabilitySampler(Math.max(0.0, Math.min(1.0, constantProbability)));
                actionBuilder.staticSampler(sampler);
            } catch (NumberFormatException e) {
                VariableAccessor probabilityAccessor = variableAccessorFactory.getVariableAccessor(sampleProbability);
                actionBuilder.dynamicSampleProbabilityAccessor(probabilityAccessor);
            }
        }
    }

    private List<IHookAction> buildTracingExitActions(RuleTracingSettings tracing) {
        val result = new ArrayList<IHookAction>();

        val attributes = tracing.getAttributes();
        if (!attributes.isEmpty()) {
            Map<String, VariableAccessor> attributeAccessors = new HashMap<>();
            attributes.forEach((attribute, variable) -> attributeAccessors.put(attribute, variableAccessorFactory.getVariableAccessor(variable)));
            IHookAction endTraceAction = new WriteSpanAttributesAction(attributeAccessors);
            IHookAction actionWithConditions = ConditionalHookAction.wrapWithConditionChecks(tracing.getAttributeConditions(), endTraceAction, variableAccessorFactory);
            result.add(actionWithConditions);
        }

        if (tracing.getEndSpan() && (tracing.getStartSpan() || tracing.getContinueSpan() != null)) {
            val endSpanAction = new EndSpanAction(ConditionalHookAction.getAsPredicate(tracing.getEndSpanConditions(), variableAccessorFactory));
            result.add(endSpanAction);
        }
        return result;
    }

    private Optional<IHookAction> buildMetricsRecorder(MethodHookConfiguration config) {
        if (!config.getConstantMetrics().isEmpty() || !config.getDataMetrics().isEmpty()) {
            Map<String, VariableAccessor> dataMetrics = new HashMap<>();
            config.getDataMetrics().forEach((metric, data) -> dataMetrics.put(metric, variableAccessorFactory.getVariableAccessor(data)));
            MetricsRecorder recorder = new MetricsRecorder(config.getConstantMetrics(), dataMetrics, metricsManager, statsRecorder);
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

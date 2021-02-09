package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.collect.HashMultiset;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.rules.RuleTracingSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.callsorting.CyclicDataDependencyException;
import rocks.inspectit.ocelot.core.instrumentation.config.callsorting.GenericActionCallSorter;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationConfiguration;
import rocks.inspectit.ocelot.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.ocelot.core.instrumentation.config.model.MethodHookConfiguration;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class MethodHookConfigurationResolver {

    private static final Predicate<Object> ALWAYS_TRUE = x -> true;

    @Autowired
    GenericActionCallSorter scheduler;

    /**
     * Derives the configuration of the hook for the given method.
     *
     * @param allSettings  The global instrumentation configuration, used for the global master switches
     * @param matchedRules All enabled rules which have a scope which matches to this method, must contain at least one value
     *
     * @return
     */
    public MethodHookConfiguration buildHookConfiguration(InstrumentationConfiguration allSettings, Set<InstrumentationRule> matchedRules)
            throws Exception {

        val result = MethodHookConfiguration.builder();

        result.preEntryActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getPreEntryActions));
        result.entryActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getEntryActions));
        result.postEntryActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getPostEntryActions));
        result.preExitActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getPreExitActions));
        result.exitActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getExitActions));
        result.postExitActions(combineAndOrderActionCalls(matchedRules, InstrumentationRule::getPostExitActions));

        if (allSettings.isMetricsEnabled()) {
            resolveMetrics(result, matchedRules);
        }

        if (allSettings.isTracingEnabled()) {
            resolveTracing(result, matchedRules);
        }

        return result.build();
    }

    private void resolveTracing(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) throws ConflictingDefinitionsException {

        val builder = RuleTracingSettings.builder();

        Set<InstrumentationRule> tracingRules = matchedRules.stream()
                .filter(r -> r.getTracing() != null)
                .collect(Collectors.toSet());

        if (!tracingRules.isEmpty()) {

            resolveStartSpan(tracingRules, builder);
            resolveEndSpan(tracingRules, builder);
            resolveContinueSpan(tracingRules, builder);
            builder.storeSpan(getAndDetectConflicts(tracingRules, r -> r.getTracing()
                    .getStoreSpan(), s -> !StringUtils.isEmpty(s), "store span data key"));
            builder.errorStatus(getAndDetectConflicts(tracingRules, r -> r.getTracing()
                    .getErrorStatus(), s -> !StringUtils.isEmpty(s), "error-status data key"));
            builder.autoTracing(getAndDetectConflicts(tracingRules, r -> r.getTracing()
                    .getAutoTracing(), Objects::nonNull, "auto tracing"));

            resolveSpanAttributeWriting(tracingRules, builder);

            result.tracing(builder.build());
        }

    }

    private void resolveSpanAttributeWriting(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        Collection<InstrumentationRule> attributeWritingRules = matchedRules.stream()
                .filter(r -> !r.getTracing().getAttributes().isEmpty())
                .collect(Collectors.toSet());
        if (!attributeWritingRules.isEmpty()) {
            Set<String> writtenAttributes = attributeWritingRules.stream()
                    .flatMap(r -> r.getTracing().getAttributes().entrySet().stream())
                    .filter(e -> !StringUtils.isEmpty(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            Map<String, String> resultAttributes = new HashMap<>();
            for (String attributeKey : writtenAttributes) {
                String dataKey = getAndDetectConflicts(attributeWritingRules, r -> r.getTracing()
                                .getAttributes()
                                .get(attributeKey),
                        x -> !StringUtils.isEmpty(x), "the span attribute'" + attributeKey + "'");
                resultAttributes.put(attributeKey, dataKey);
            }
            builder.attributes(resultAttributes);
            builder.attributeConditions(getAndDetectConflicts(attributeWritingRules, r -> r.getTracing()
                    .getAttributeConditions(), ALWAYS_TRUE, "span attribute writing conditions"));
        }
    }

    private void resolveContinueSpan(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        Set<InstrumentationRule> rulesContinuingSpan = matchedRules.stream()
                .filter(r -> r.getTracing().getContinueSpan() != null)
                .collect(Collectors.toSet());
        String continueSpan = getAndDetectConflicts(rulesContinuingSpan, r -> r.getTracing()
                .getContinueSpan(), ALWAYS_TRUE, "continue-span");
        builder.continueSpan(continueSpan);
        if (continueSpan != null) {
            builder.continueSpanConditions(getAndDetectConflicts(rulesContinuingSpan, r -> r.getTracing()
                    .getContinueSpanConditions(), ALWAYS_TRUE, "continue span conditions"));
        }
    }

    private void resolveEndSpan(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        Set<InstrumentationRule> rulesDefiningEndSpan = matchedRules.stream()
                .filter(r -> r.getTracing().getEndSpan() != null)
                .collect(Collectors.toSet());
        Boolean endSpanSetting = getAndDetectConflicts(rulesDefiningEndSpan, r -> r.getTracing()
                .getEndSpan(), Objects::nonNull, "end-span");
        boolean endSpan = Optional.ofNullable(endSpanSetting).orElse(true);
        builder.endSpan(endSpan);
        if (endSpan) {
            builder.endSpanConditions(
                    Optional.ofNullable(
                            getAndDetectConflicts(rulesDefiningEndSpan, r -> r.getTracing()
                                    .getEndSpanConditions(), ALWAYS_TRUE, "end span conditions")
                    ).orElse(new ConditionalActionSettings()));
        }
    }

    private void resolveStartSpan(Set<InstrumentationRule> matchedRules, RuleTracingSettings.RuleTracingSettingsBuilder builder) throws ConflictingDefinitionsException {
        Set<InstrumentationRule> rulesDefiningStartSpan = matchedRules.stream()
                .filter(r -> r.getTracing().getStartSpan() != null)
                .collect(Collectors.toSet());
        Boolean startSpanSetting = getAndDetectConflicts(rulesDefiningStartSpan, r -> r.getTracing()
                .getStartSpan(), ALWAYS_TRUE, "start-span");
        boolean startSpan = Optional.ofNullable(startSpanSetting).orElse(false);
        builder.startSpan(startSpan);
        if (startSpan) {
            builder.startSpanConditions(getAndDetectConflicts(rulesDefiningStartSpan, r -> r.getTracing()
                    .getStartSpanConditions(), ALWAYS_TRUE, "start span conditions"));
            //name, kind and sample probability can be defined by rules which do not start a span themselves
            builder.name(getAndDetectConflicts(matchedRules, r -> r.getTracing()
                    .getName(), n -> !StringUtils.isEmpty(n), "the span name"));
            builder.kind(getAndDetectConflicts(matchedRules, r -> r.getTracing()
                    .getKind(), Objects::nonNull, "the span kind"));
            builder.sampleProbability(getAndDetectConflicts(matchedRules, r -> r.getTracing()
                    .getSampleProbability(), n -> !StringUtils.isEmpty(n), "the trace sample probability"));
        }
    }

    /**
     * Utility function for merging configurations from multiple rules and detecting conflicts.
     * This method first calls the given getter on all specified rules and filters the results using the given filter.
     * <p>
     * It then ensures that all provided values are equal, otherwise raises an exception with the given message
     *
     * @param rules            the rules on which the getter will be called
     * @param getter           the getter function to call on each rule
     * @param filter           the predicate to filter the results of the getters with, e.g. Objects#nonNull
     * @param exceptionMessage the name of the setting to print in an exception message
     * @param <T>              the type of the value which is being queried
     *
     * @return null if none of the rules have a setting matching the given filter. Otherwise returns the setting found.
     *
     * @throws ConflictingDefinitionsException thrown if a conflicting setting is detected
     */
    private <T> T getAndDetectConflicts(Collection<InstrumentationRule> rules, Function<InstrumentationRule, T> getter, Predicate<? super T> filter, String exceptionMessage)
            throws ConflictingDefinitionsException {

        Optional<InstrumentationRule> firstMatch = rules.stream().filter(r -> filter.test(getter.apply(r))).findFirst();
        if (firstMatch.isPresent()) {
            T value = getter.apply(firstMatch.get());
            Optional<InstrumentationRule> secondMatch = rules.stream()
                    .filter(r -> r != firstMatch.get())
                    .filter(r -> filter.test(getter.apply(r)))
                    .filter(r -> !Objects.equals(getter.apply(r), value))
                    .findFirst();
            if (secondMatch.isPresent()) {
                throw new ConflictingDefinitionsException(firstMatch.get(), secondMatch.get(), exceptionMessage);
            } else {
                return value;
            }
        } else {
            return null;
        }
    }

    /**
     * Combines all metric definitions from the given rules
     *
     * @param result       the hook configuration to which the measurement definitions are added
     * @param matchedRules the rules to combine
     */
    private void resolveMetrics(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) {
        result.metrics(matchedRules.stream()
                .flatMap(rule -> rule.getMetrics().stream())
                .collect(Collectors.toCollection(HashMultiset::create)));
    }

    /**
     * Combines and correctly orders all action calls from the given rules to a single ordered sequence of calls
     *
     * @param rules         the rules whose generic action calls should be merged
     * @param actionsGetter the getter to access the rules to process, e.g. {@link InstrumentationRule#getEntryActions()}
     *
     * @return a map mapping the data keys to the action call which define the values
     *
     * @throws CyclicDataDependencyException if the action calls have cyclic dependencies preventing a scheduling
     */
    private List<ActionCallConfig> combineAndOrderActionCalls(Set<InstrumentationRule> rules, Function<InstrumentationRule, Collection<ActionCallConfig>> actionsGetter)
            throws CyclicDataDependencyException {

        List<ActionCallConfig> allCalls = rules.stream()
                .map(actionsGetter)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return scheduler.orderActionCalls(allCalls);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    static class ConflictingDefinitionsException extends Exception {

        /**
         * The first rule assigning a value to dataKey
         */
        private InstrumentationRule first;

        /**
         * The second rule assigning a value to dataKey
         */
        private InstrumentationRule second;

        private String messageSuffix;

        @Override
        public String getMessage() {
            return "The rules '" + first.getName() + "' and '" + second.getName() + "' contain conflicting definitions for " + messageSuffix;
        }
    }
}

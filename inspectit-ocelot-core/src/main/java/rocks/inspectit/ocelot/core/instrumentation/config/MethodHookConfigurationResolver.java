package rocks.inspectit.ocelot.core.instrumentation.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ConditionalActionSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class MethodHookConfigurationResolver {

    @Autowired
    DataProviderCallSorter scheduler;

    /**
     * Derives the configuration of the hook for the given method.
     *
     * @param method       The method to derive the hook for
     * @param matchedRules All enabled rules which have a scope which matches to this method, must contain at least one value
     * @return
     */
    public MethodHookConfiguration buildHookConfiguration(Class<?> clazzToInstrument, MethodDescription method, InstrumentationConfiguration allSettings, Set<InstrumentationRule> matchedRules)
            throws Exception {

        val result = MethodHookConfiguration.builder();
        result.entryProviders(combineAndOrderProviderCalls(matchedRules, InstrumentationRule::getEntryProviders));
        result.exitProviders(combineAndOrderProviderCalls(matchedRules, InstrumentationRule::getExitProviders));

        if (allSettings.isMetricsEnabled()) {
            resolveMetrics(result, matchedRules);
        }

        if (allSettings.isTracingEnabled()) {
            resolveTracing(result, matchedRules);
        }

        return result.build();
    }

    private void resolveTracing(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) throws ConflictingDefinitionsException {
        Collection<InstrumentationRule> spanStartingRules = matchedRules.stream().filter(r -> r.getTracing().isStartSpan()).collect(Collectors.toSet());
        boolean startSpan = !spanStartingRules.isEmpty();

        val builder = MethodTracingConfiguration.builder().startSpan(startSpan);
        if (startSpan) {
            builder.spanNameDataKey(
                    getAndDetectConflicts(
                            spanStartingRules,
                            r -> r.getTracing().getName(),
                            n -> !StringUtils.isEmpty(n),
                            "the span name"));
            builder.spanKind(getAndDetectConflicts(spanStartingRules, r -> r.getTracing().getKind(), Objects::nonNull, "the span kind"));
        }

        Set<String> writtenAttributes = matchedRules.stream()
                .flatMap(r -> r.getTracing().getAttributes().entrySet().stream())
                .filter(e -> !StringUtils.isEmpty(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (String attributeKey : writtenAttributes) {
            String dataKey = getAndDetectConflicts(matchedRules, r -> r.getTracing().getAttributes().get(attributeKey),
                    x -> !StringUtils.isEmpty(x), "the span attribute'" + attributeKey + "'");
            builder.attribute(attributeKey, dataKey);
        }


        ConditionalActionSettings conditions = new ConditionalActionSettings();
        conditions.setOnlyIfFalse(getAndDetectConflicts(matchedRules, r -> r.getTracing().getOnlyIfFalse(), Objects::nonNull,
                "the only-if-false tracing condition"));
        conditions.setOnlyIfTrue(getAndDetectConflicts(matchedRules, r -> r.getTracing().getOnlyIfTrue(), Objects::nonNull,
                "the only-if-true tracing condition"));
        conditions.setOnlyIfNull(getAndDetectConflicts(matchedRules, r -> r.getTracing().getOnlyIfNull(), Objects::nonNull,
                "the only-if-null tracing condition"));
        conditions.setOnlyIfNotNull(getAndDetectConflicts(matchedRules, r -> r.getTracing().getOnlyIfNotNull(), Objects::nonNull,
                "the only-if-not-null tracing condition"));
        builder.conditions(conditions);
        result.tracing(builder.build());
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
     * @return null if none of the rules have a setting matching the given filter. Otherwise returns the setting found.
     * @throws ConflictingDefinitionsException thrown if a conflicting setting is detected
     */
    private <T> T getAndDetectConflicts(Collection<InstrumentationRule> rules, Function<InstrumentationRule, T> getter, Predicate<T> filter, String exceptionMessage)
            throws ConflictingDefinitionsException {

        Optional<InstrumentationRule> firstMatch = rules.stream().filter(r -> filter.test(getter.apply(r))).findFirst();
        if (firstMatch.isPresent()) {
            T value = getter.apply(firstMatch.get());
            Optional<InstrumentationRule> secondMatch = rules.stream()
                    .filter(r -> r != firstMatch.get())
                    .filter(r -> filter.test(getter.apply(r)))
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
     * @throws ConflictingDefinitionsException of two rules define different values for the same metric
     */
    private void resolveMetrics(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) throws ConflictingDefinitionsException {

        Map<String, InstrumentationRule> metricDefinitions = new HashMap<>();
        for (val rule : matchedRules) {
            //check for conflicts first
            for (val metricName : rule.getMetrics().keySet()) {
                if (metricDefinitions.containsKey(metricName)) {
                    throw new ConflictingDefinitionsException(metricDefinitions.get(metricName), rule, "the metric '" + metricName + "'");
                }
                metricDefinitions.put(metricName, rule);
            }
            rule.getMetrics().forEach((name, value) -> {
                try {
                    double constantValue = Double.parseDouble(value);
                    result.constantMetric(name, constantValue);
                } catch (NumberFormatException e) {
                    //the specified value is not a double value, we therefore assume it is a data key
                    result.dataMetric(name, value);
                }
            });
        }
    }

    /**
     * Combines and correctly orders all data provider calls from the given rules to a single map
     *
     * @param rules           the rules whose data-provider calls should be merged
     * @param providersGetter the getter to access the rules to process, e.g. {@link InstrumentationRule#getEntryProviders()}
     * @return a map mapping the data keys to the provider call which define the values
     * @throws ConflictingDefinitionsException                      if the same data key is defined with different data-provider calls
     * @throws DataProviderCallSorter.CyclicDataDependencyException if the provider calls have cyclic dependencies preventing a scheduling
     */
    private List<DataProviderCallConfig> combineAndOrderProviderCalls(Set<InstrumentationRule> rules, Function<InstrumentationRule, Collection<DataProviderCallConfig>> providersGetter)
            throws ConflictingDefinitionsException, DataProviderCallSorter.CyclicDataDependencyException {
        Map<String, InstrumentationRule> dataOrigins = new HashMap<>();
        Map<String, DataProviderCallConfig> dataDefinitions = new HashMap<>();
        for (val rule : rules) {
            Collection<DataProviderCallConfig> providers = providersGetter.apply(rule);
            for (val dataDefinition : providers) {
                String dataKey = dataDefinition.getName();

                //check if we have previously already encountered a differing definition for the key
                if (dataOrigins.containsKey(dataKey) && !dataDefinition.equals(dataDefinitions.get(dataKey))) {
                    throw new ConflictingDefinitionsException(dataOrigins.get(dataKey), rule, "the data key '" + dataKey + "'");
                }

                dataOrigins.put(dataKey, rule);
                dataDefinitions.put(dataKey, dataDefinition);
            }
        }

        return scheduler.orderDataProviderCalls(dataDefinitions.values());
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

package rocks.inspectit.oce.core.instrumentation.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.instrumentation.config.model.DataProviderCallConfig;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class MethodHookConfigurationResolver {

    @Autowired
    DataProviderGenerator providerGenerator;

    @Autowired
    DataProviderCallSorter scheduler;

    /**
     * Derives the configuration of the hook for the given method.
     *
     * @param method       The method to derive the hook for
     * @param matchedRules All enabled rules which have a scope which matches to this method, must contain at least one value
     * @return
     */
    public MethodHookConfiguration buildHookConfiguration(Class<?> clazzToInstrument, MethodDescription method, Set<InstrumentationRule> matchedRules)
            throws Exception {

        val result = MethodHookConfiguration.builder();
        result.entryProviders(combineAndOrderProviderCalls(matchedRules, InstrumentationRule::getEntryProviders));
        result.exitProviders(combineAndOrderProviderCalls(matchedRules, InstrumentationRule::getExitProviders));

        resolveMetrics(result, matchedRules);

        return result.build();
    }

    /**
     * Combines all metric definitions from the given rules
     *
     * @param result       the hook configuration to which the measurement definitions are added
     * @param matchedRules the rules to combine
     * @throws ConflictingMetricDefinitionsException of two rules define different values for the same metric
     */
    private void resolveMetrics(MethodHookConfiguration.MethodHookConfigurationBuilder result, Set<InstrumentationRule> matchedRules) throws ConflictingMetricDefinitionsException {

        Map<String, InstrumentationRule> metricDefinitions = new HashMap<>();
        for (val rule : matchedRules) {
            //check for conflicts first
            for (val metricName : rule.getMetrics().keySet()) {
                if (metricDefinitions.containsKey(metricName)) {
                    throw new ConflictingMetricDefinitionsException(metricName, metricDefinitions.get(metricName), rule);
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
     * @throws ConflictingDataDefinitionsException                  if the same data key is defined with different data-provider calls
     * @throws DataProviderCallSorter.CyclicDataDependencyException if the provider calls have cyclic dependencies preventing a scheduling
     */
    private List<Pair<String, DataProviderCallConfig>> combineAndOrderProviderCalls(Set<InstrumentationRule> rules, Function<InstrumentationRule, Map<String, DataProviderCallConfig>> providersGetter)
            throws ConflictingDataDefinitionsException, DataProviderCallSorter.CyclicDataDependencyException {
        Map<String, InstrumentationRule> dataOrigins = new HashMap<>();
        Map<String, DataProviderCallConfig> dataDefinitions = new HashMap<>();
        for (val rule : rules) {
            Map<String, DataProviderCallConfig> providers = providersGetter.apply(rule);
            for (val dataDefinition : providers.entrySet()) {
                String dataKey = dataDefinition.getKey();
                DataProviderCallConfig call = dataDefinition.getValue();

                //check if we have previously already encountered a differing definition for the key
                if (dataOrigins.containsKey(dataKey) && !call.equals(dataDefinitions.get(dataKey))) {
                    throw new ConflictingDataDefinitionsException(dataKey, dataOrigins.get(dataKey), rule);
                }

                dataOrigins.put(dataKey, rule);
                dataDefinitions.put(dataKey, call);
            }
        }

        return scheduler.orderDataProviderCalls(dataDefinitions);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    static class ConflictingDataDefinitionsException extends Exception {
        private String dataKey;
        /**
         * The first rule assigning a value to dataKey
         */
        private InstrumentationRule first;
        /**
         * The second rule assigning a value to dataKey
         */
        private InstrumentationRule second;

        @Override
        public String getMessage() {
            return "The rules '" + first.getName() + "' and '" + second.getName() + "' contain conflicting definitions for the data '" + dataKey + "'";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    static class ConflictingMetricDefinitionsException extends Exception {
        private String metricName;
        /**
         * The first rule assigning a value to dataKey
         */
        private InstrumentationRule first;
        /**
         * The second rule assigning a value to dataKey
         */
        private InstrumentationRule second;

        @Override
        public String getMessage() {
            return "The rules '" + first.getName() + "' and '" + second.getName() + "' contain conflicting definitions for the metric '" + metricName + "'";
        }
    }
}

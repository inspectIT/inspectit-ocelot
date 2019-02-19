package rocks.inspectit.oce.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.val;
import net.bytebuddy.description.method.MethodDescription;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.instrumentation.config.model.InstrumentationRule;
import rocks.inspectit.oce.core.instrumentation.config.model.MethodHookConfiguration;
import rocks.inspectit.oce.core.instrumentation.config.model.ResolvedDataProviderCall;
import rocks.inspectit.oce.core.instrumentation.dataprovider.generic.DataProviderGenerator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MethodHookConfigurationResolver {

    @Autowired
    DataProviderGenerator providerGenerator;

    /**
     * Derives the configuration of the hook for the given method.
     *
     * @param method       The method to derive the hook for
     * @param matchedRules All enabled rules which have a scope which matches to this method, must contain at least one value
     * @return
     */
    public MethodHookConfiguration buildHookConfiguration(Class<?> clazzToInstrument, MethodDescription method, Set<InstrumentationRule> matchedRules)
            throws CyclicDataDependencyException, ConflictingDataDefinitionsException {

        val result = MethodHookConfiguration.builder();
        result.entryProviders(combineAndOrderProviderCalls(matchedRules, InstrumentationRule::getEntryProviders));
        result.exitProviders(combineAndOrderProviderCalls(matchedRules, InstrumentationRule::getExitProviders));

        return result.build();
    }

    /**
     * Combines all data providers from the given rules which are accessible using the given getter.
     * The data provider calls are checked for conflicts and ordered correctly.
     *
     * @param rules           the rules whose data provider calls should be combined
     * @param providersGetter the accessor for getting the data providers from teh rules, e.g. {@link InstrumentationRule#getEntryProviders()}
     * @return the ordered list of data keys with their corresponding data provider call
     * @throws ConflictingDataDefinitionsException
     * @throws CyclicDataDependencyException
     */
    private List<Pair<String, ResolvedDataProviderCall>> combineAndOrderProviderCalls(Set<InstrumentationRule> rules, Function<InstrumentationRule, Map<String, ResolvedDataProviderCall>> providersGetter)
            throws ConflictingDataDefinitionsException, CyclicDataDependencyException {

        Map<String, ResolvedDataProviderCall> dataDefinitions = combineProvidersFromRules(rules, providersGetter);

        Function<String, Collection<String>> getDependencies = key -> dataDefinitions.get(key).getCallSettings().getDataInput().values().stream()
                //using the same data as in & outout is allowed
                //this means that a down-propagated value is used as input
                .filter(otherKey -> !key.equals(otherKey))
                //only consider the relevant keys as dependencies
                .filter(dataDefinitions::containsKey)
                .collect(Collectors.toSet());
        List<String> sortedDataAssignments = getInTopologicalOrder(dataDefinitions.keySet(), getDependencies);

        return sortedDataAssignments.stream()
                .map(key -> Pair.of(key, dataDefinitions.get(key)))
                .collect(Collectors.toList());

    }

    private Map<String, ResolvedDataProviderCall> combineProvidersFromRules(Set<InstrumentationRule> rules, Function<InstrumentationRule, Map<String, ResolvedDataProviderCall>> providersGetter) throws ConflictingDataDefinitionsException {
        Map<String, InstrumentationRule> dataOrigins = new HashMap<>();
        Map<String, ResolvedDataProviderCall> dataDefinitions = new HashMap<>();
        for (val rule : rules) {
            Map<String, ResolvedDataProviderCall> providers = providersGetter.apply(rule);
            for (val dataDefinition : providers.entrySet()) {
                String dataKey = dataDefinition.getKey();
                ResolvedDataProviderCall call = dataDefinition.getValue();

                if (dataOrigins.containsKey(dataKey) && !call.equals(dataDefinitions.get(dataKey))) {
                    throw new ConflictingDataDefinitionsException(dataKey, dataOrigins.get(dataKey), rule);
                }

                dataOrigins.put(dataKey, rule);
                dataDefinitions.put(dataKey, call);
            }
        }
        return dataDefinitions;
    }

    /**
     * Topologically sorts the given data keys using a depth-first search.
     * Topological order means that the dependencies of a data appear prior to the data itself.
     *
     * @param dataKeys     the data keys to sort
     * @param dependencies maps every data key to its dependencies
     * @return a sorted list containing all data keys
     * @throws CyclicDataDependencyException if there is a cyclic dependency, topological sorting is not possible
     */
    @VisibleForTesting
    List<String> getInTopologicalOrder(Collection<String> dataKeys, Function<String, Collection<String>> dependencies) throws CyclicDataDependencyException {
        Map<String, Set<String>> dependenciesMap =
                dataKeys.stream()
                        .collect(Collectors.toMap(
                                key -> key,
                                key -> dependencies.apply(key)
                                        .stream()
                                        .filter(otherKey -> !otherKey.equals(key))
                                        .collect(Collectors.toSet())));

        StringStack result = new StringStack();
        StringStack stackTrace = new StringStack();

        for (String dataKey : dataKeys) {
            putInTopologicalOrder(result, dataKey, dependenciesMap, stackTrace);
        }
        return result.getList();
    }

    private void putInTopologicalOrder(StringStack result, String current, Map<String, Set<String>> dependenciesMap, StringStack stackTrace) throws CyclicDataDependencyException {

        if (result.contains(current)) {
            return; //this key has already been processed
        }

        stackTrace.push(current);
        for (String dependency : dependenciesMap.getOrDefault(current, Collections.emptySet())) {
            if (!stackTrace.contains(dependency)) {
                putInTopologicalOrder(result, dependency, dependenciesMap, stackTrace);
            } else {
                ArrayList<String> stackTraceList = stackTrace.getList();
                int idx = stackTraceList.indexOf(dependency);
                throw new CyclicDataDependencyException(stackTraceList.subList(idx, stackTraceList.size()));
            }
        }
        result.push(current);
        stackTrace.removeTop();
    }

    static class StringStack {
        @Getter
        private ArrayList<String> list = new ArrayList<>();

        private HashSet<String> set = new HashSet<>();

        void push(String str) {
            list.add(str);
            set.add(str);
        }

        void removeTop() {
            String str = list.remove(list.size() - 1);
            set.remove(str);
        }

        boolean contains(String str) {
            return set.contains(str);
        }
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
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    static class CyclicDataDependencyException extends Exception {
        /**
         * A list of data keys representing the dependency cycle.
         */
        private List<String> dependencyCycle;
    }
}

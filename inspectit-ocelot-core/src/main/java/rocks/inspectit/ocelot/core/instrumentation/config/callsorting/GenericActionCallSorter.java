package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class encapsulates the logic how action calls are ordered.
 * The approach is that first a dependency graph is generated.
 * In this dependency graph the nodes are the ActionCalls,
 * the edges indicate a dependency between them.
 * The edges are direct and an edge from call_A to call_B reads as
 * "The call_B must be executed before the call_A"
 * <p>
 * Dependencies occur implicity due to the name of the call (= the result data key written)
 * the used input data ({@link ActionCallSettings#getDataInput()}, {@link ActionCallSettings#getOnlyIfTrue()}, etc)
 * and the explicitly defined dependencies: ({@link ActionCallSettings#getReads()}, {@link ActionCallSettings#getWrites()},
 * {@link ActionCallSettings#getReadsBeforeWritten()}).
 * <p>
 * The ordering happens so that the following three constraints are always fulfilled:
 * <p>
 * A) a call which reads AND does NOT write a data key is executed after any call writing this specific data key
 * B) a call which reads AND writes a data key is executed after any call writing AND not reading this specific data key
 * C) a call specifying reads-before-written for a data key is executed before any call writing this specific data key
 * <p>
 * Rule B) allows to do "pipeline" processing.
 * E.g. calls can take "http_path" as in- and output in order to remove path variables from it.
 * Any action only reading "http_path" will then have access to the parametrized path
 */
@Component
public class GenericActionCallSorter {

    /**
     * Orders the given set of action calls based on their inter-dependencies.
     *
     * @param calls the calls to sort
     * @return the sorted calls, meaning that the actions should be executed in the order in which they appear in the list
     * @throws CyclicDataDependencyException if the action calls have a cyclic dependency and therefore cannot be ordered
     */
    public List<ActionCallConfig> orderActionCalls(Collection<ActionCallConfig> calls) throws CyclicDataDependencyException {

        Map<ActionCallConfig, Set<ActionCallConfig>> dependencyGraph = buildDependencyGraph(calls);

        return getInTopologicalOrder(dependencyGraph);
    }

    private Map<ActionCallConfig, Set<ActionCallConfig>> buildDependencyGraph(Collection<ActionCallConfig> calls) {

        List<CallDependencies> dependencies = calls.stream()
                .map(CallDependencies::collectFor)
                .collect(Collectors.toList());

        CallDependencyIndex index = new CallDependencyIndex();
        dependencies.forEach(index::add);

        return dependencies.stream()
                .collect(Collectors.toMap(CallDependencies::getSource, call -> getActionDependencies(call, index)));
    }

    /**
     * Returns all calls on which the given actioncall depends.
     *
     * @param call  the data-dependencies of the call
     * @param index the index to use for querying.
     * @return a set of all ActionCallConfigs, on which the ActionCallConfig represented by "call" depends.
     */
    private Set<ActionCallConfig> getActionDependencies(CallDependencies call, CallDependencyIndex index) {
        //Rule: calls ONLY reading (and not writing) a data key are executed after any call writing the given data key
        Stream<CallDependencies> readOnlyDeps = call.getReads().stream()
                .filter(key -> !call.getWrites().contains(key))
                .flatMap(key -> index.getCallsWriting(key).stream());

        //Rule: calls reading AND writing data keys are executed after any call ONLY writing (and not reading) the given data key
        Stream<CallDependencies> readAndWriteDeps = call.getReads().stream()
                .filter(key -> call.getWrites().contains(key))
                .flatMap(key ->
                        index.getCallsWriting(key).stream()
                                .filter(depCall -> !depCall.getReads().contains(key))
                );

        //Rule: calls writing a data key are executed after any call with a "reads-before-overriden" on the corresponding key
        Stream<CallDependencies> writeDeps = call.getWrites().stream()
                .flatMap(key -> index.getCallsReadingBeforeOverridden(key).stream());

        return Stream.concat(readOnlyDeps, Stream.concat(readAndWriteDeps, writeDeps))
                .map(CallDependencies::getSource)
                .collect(Collectors.toSet());
    }


    /**
     * Topologically sorts the given data keys using a depth-first search.
     * Topological order means that the dependencies of a data appear prior to the data itself.
     *
     * @param dependencyGraph a map mapping each data key to the data keys he depends on. This map defines the the dependency graph.
     *                        The map is allowed to contain dependencies to data keys which are not part of dataKeys, those are simply ignored.
     * @return a sorted list containing all data keys
     * @throws CyclicDataDependencyException if there is a cyclic dependency, topological sorting is not possible
     */
    private List<ActionCallConfig> getInTopologicalOrder(Map<ActionCallConfig, Set<ActionCallConfig>> dependencyGraph) throws CyclicDataDependencyException {

        //make the order deterministic to get deterministic error messages, e.g. in case of multiple dependency cycles
        List<Pair<ActionCallConfig, List<ActionCallConfig>>> sortedDependencyGraph = sortDependencyGraph(dependencyGraph);
        Map<ActionCallConfig, List<ActionCallConfig>> sortedLookup =
                sortedDependencyGraph.stream()
                        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        ActionCallStack result = new ActionCallStack();
        //we remember which nodes we visited via a "stack-trace" to detect dependency cycles
        ActionCallStack stackTrace = new ActionCallStack();


        for (Pair<ActionCallConfig, List<ActionCallConfig>> callWithDependencies : sortedDependencyGraph) {
            putInTopologicalOrder(result, callWithDependencies.getLeft(), sortedLookup, stackTrace);
        }
        return result.getList();
    }

    private List<Pair<ActionCallConfig, List<ActionCallConfig>>> sortDependencyGraph(Map<ActionCallConfig, Set<ActionCallConfig>> dependencyGraph) {
        List<Pair<ActionCallConfig, List<ActionCallConfig>>> sorted = new ArrayList<>();
        dependencyGraph.forEach((call, deps) -> {
            ArrayList<ActionCallConfig> sortedDeps = new ArrayList<>(deps);
            sortedDeps.sort(Comparator.comparing(ActionCallConfig::getName));
            sorted.add(Pair.of(call, sortedDeps));
        });
        sorted.sort(Comparator.comparing(p -> p.getLeft().getName()));
        return sorted;
    }

    private void putInTopologicalOrder(ActionCallStack result,
                                       ActionCallConfig current,
                                       Map<ActionCallConfig, ? extends Collection<ActionCallConfig>> dependencyGraph,
                                       ActionCallStack stackTrace) throws CyclicDataDependencyException {

        if (result.contains(current)) {
            return; //this action call has already been processed
        }

        stackTrace.push(current);
        for (ActionCallConfig dependency : dependencyGraph.get(current)) {
            //ignore dependencies to itself
            if (!dependency.equals(current)) {
                if (!stackTrace.contains(dependency)) {
                    putInTopologicalOrder(result, dependency, dependencyGraph, stackTrace);
                } else {
                    ArrayList<ActionCallConfig> stackTraceList = stackTrace.getList();
                    int idx = stackTraceList.indexOf(dependency);
                    throw new CyclicDataDependencyException(stackTraceList.subList(idx, stackTraceList.size())
                            .stream()
                            .map(ActionCallConfig::getName)
                            .collect(Collectors.toList()));
                }
            }
        }
        result.push(current);
        stackTrace.removeTop();
    }

    private static class ActionCallStack {
        @Getter
        private ArrayList<ActionCallConfig> list = new ArrayList<>();

        private HashSet<ActionCallConfig> set = new HashSet<>();

        void push(ActionCallConfig call) {
            list.add(call);
            set.add(call);
        }

        void removeTop() {
            ActionCallConfig call = list.remove(list.size() - 1);
            set.remove(call);
        }

        boolean contains(ActionCallConfig call) {
            return set.contains(call);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class CyclicDataDependencyException extends Exception {
        /**
         * A list of data keys representing the dependency cycle.
         */
        private List<String> dependencyCycle;
    }
}

package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.config.model.instrumentation.actions.OrderSettings;
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
 * and the explicitly defined dependencies: ({@link OrderSettings#getReads()}, {@link OrderSettings#getWrites()},
 * {@link OrderSettings#getReadsBeforeWritten()}).
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
        Stream<CallDependencies> readOnlyDependencies = call.getReads().stream()
                .filter(key -> !call.getWrites().contains(key))
                .flatMap(key -> index.getCallsWriting(key).stream());

        //Rule: calls reading AND writing data keys are executed after any call ONLY writing (and not reading) the given data key
        Stream<CallDependencies> readAndWriteDependencies = call.getReads().stream()
                .filter(key -> call.getWrites().contains(key))
                .flatMap(key ->
                        index.getCallsWriting(key).stream()
                                .filter(depCall -> !depCall.getReads().contains(key))
                );

        //Rule: calls writing a data key are executed after any call with a "reads-before-overriden" on the corresponding key
        Stream<CallDependencies> writeDependencies = call.getWrites().stream()
                .flatMap(key -> index.getCallsReadingBeforeWritten(key).stream());

        return Stream.concat(readOnlyDependencies, Stream.concat(readAndWriteDependencies, writeDependencies))
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

        LinkedHashSet<ActionCallConfig> result = new LinkedHashSet<>();

        for (Pair<ActionCallConfig, List<ActionCallConfig>> callWithDependencies : sortedDependencyGraph) {
            putInTopologicalOrder(result, callWithDependencies.getLeft(), sortedLookup, new LinkedHashSet<>());
        }
        return new ArrayList<>(result);
    }

    /**
     * Sorts the edges in the given dependency graph by name.
     * This ensures a deterministic order of the action calls.
     *
     * @param dependencyGraph the unsorted dependency graph
     * @return the sorted graph
     */
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

    /**
     * Puts all dependencies of "current" ActionCallConfig in transitive order onto the result stack.
     *
     * @param result          the resulting LinkedHashSet to put the calls onto in topological order
     * @param current         the ActionCall for which all dependencies shall be put onto the result stack
     * @param dependencyGraph the dependency graph, mapping calls to all calls which should be executed prior to them
     * @param stackTrace      stack trace of visited calls, only used to detect cyclic dependencies
     * @throws CyclicDataDependencyException
     */
    private void putInTopologicalOrder(LinkedHashSet<ActionCallConfig> result,
                                       ActionCallConfig current,
                                       Map<ActionCallConfig, ? extends Collection<ActionCallConfig>> dependencyGraph,
                                       LinkedHashSet<ActionCallConfig> stackTrace) throws CyclicDataDependencyException {

        if (result.contains(current)) {
            return; //this action call has already been processed
        }

        stackTrace.add(current);
        for (ActionCallConfig dependency : dependencyGraph.get(current)) {
            //ignore dependencies to itself
            if (!dependency.equals(current)) {
                if (!stackTrace.contains(dependency)) {
                    putInTopologicalOrder(result, dependency, dependencyGraph, stackTrace);
                } else {
                    ArrayList<ActionCallConfig> stackTraceList = new ArrayList<>(stackTrace);
                    int idx = stackTraceList.indexOf(dependency);
                    List<String> dependencyCycle = stackTraceList.subList(idx, stackTraceList.size())
                            .stream()
                            .map(ActionCallConfig::getName)
                            .collect(Collectors.toList());
                    throw new CyclicDataDependencyException(dependencyCycle);
                }
            }
        }
        result.add(current);
        stackTrace.remove(current);
    }
}

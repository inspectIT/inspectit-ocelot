package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple index allowing O(1) lookup of {@link CallDependencies}.
 */
public class CallDependencyIndex {

    /**
     * Maps data keys to all CallDependencies objects writing the given data key.
     */
    private HashMap<String, Set<CallDependencies>> writesIndex = new HashMap<>();

    /**
     * Maps data keys to all CallDependencies objects having a "reads-before-written" dependency on them.
     */
    private HashMap<String, Set<CallDependencies>> readsBeforeWrittenIndex = new HashMap<>();

    /**
     * Adds the given dependencies to this index.
     *
     * @param actionCall the dependencies of a call to add.
     */
    public void add(CallDependencies actionCall) {
        actionCall.getWrites().forEach(key ->
                writesIndex
                        .computeIfAbsent(key, (k) -> new HashSet<>())
                        .add(actionCall));
        actionCall.getReadsBeforeWritten().forEach(key ->
                readsBeforeWrittenIndex
                        .computeIfAbsent(key, (k) -> new HashSet<>())
                        .add(actionCall));
    }


    /**
     * Returns all calls which request to read the given data-key before it is written (e.g. to read down-propagated values).
     *
     * @param dataKey the data-key to check for
     * @return all calls having a reads-before-written dependency on the given data key.
     */
    public Set<CallDependencies> getCallsReadingBeforeWritten(String dataKey) {
        return readsBeforeWrittenIndex.getOrDefault(dataKey, Collections.emptySet());
    }


    /**
     * Returns all calls which write the given data-key.
     *
     * @param dataKey the data-key to check for
     * @return all calls having a writes dependency on the given data key.
     */
    public Set<CallDependencies> getCallsWriting(String dataKey) {
        return writesIndex.getOrDefault(dataKey, Collections.emptySet());
    }
}

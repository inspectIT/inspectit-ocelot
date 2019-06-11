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


    public Set<CallDependencies> getCallsReadingBeforeOverridden(String dataKey) {
        return readsBeforeWrittenIndex.getOrDefault(dataKey, Collections.emptySet());
    }

    public Set<CallDependencies> getCallsWriting(String dataKey) {
        return writesIndex.getOrDefault(dataKey, Collections.emptySet());
    }
}

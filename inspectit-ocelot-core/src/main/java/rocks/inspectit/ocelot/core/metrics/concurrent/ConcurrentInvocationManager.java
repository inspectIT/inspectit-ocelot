package rocks.inspectit.ocelot.core.metrics.concurrent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the current amount of active invocations for specific operations.
 * The operation name is used as key and servers just a label to group and filter for specific invocations.
 * Furthermore, we include the operation name as OpenCensus tag to the measurement.
 */
@Component
public class ConcurrentInvocationManager {

    private final ConcurrentHashMap<String, Long> activeInvocations = new ConcurrentHashMap<>();

    /**
     * @return the currently active invocations
     */
    public Map<String, Long> getActiveInvocations() {
        return activeInvocations;
    }

    /**
     * Adds one invocation to the specified operation
     *
     * @param operation the name of the invoked scope
     */
    public void addInvocation(String operation) {
        activeInvocations.merge(operation, 1L, Long::sum);
    }

    /**
     * Removes one invocation from the specified operation.
     * If the resulting amount of invocations reaches 0, the entry will be removed
     *
     * @param operation the name of the invoked operation
     */
    public void removeInvocation(String operation) {
        activeInvocations.compute(operation, (key, count) -> {
            if (count == null || count <= 1) return null; // remove entry
            return count - 1L;
        });
    }
}
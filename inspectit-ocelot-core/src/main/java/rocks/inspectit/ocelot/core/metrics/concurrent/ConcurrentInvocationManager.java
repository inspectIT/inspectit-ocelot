package rocks.inspectit.ocelot.core.metrics.concurrent;

import com.google.common.annotations.VisibleForTesting;
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

    /**
     * Default operation name, if nothing was specified by the user
     */
    @VisibleForTesting
    static final String DEFAULT_OPERATION = "default";

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
     * @param operation the name of the invoked operation
     */
    public void addInvocation(String operation) {
        if(operation == null || operation.isEmpty())
            activeInvocations.merge(DEFAULT_OPERATION, 1L, Long::sum);
        else
            activeInvocations.merge(operation, 1L, Long::sum);
    }

    /**
     * Removes one invocation from the specified operation. The minimum amount of invocations is 0. <br>
     * We do not want to delete the entry, because we still want to export the metric value 0.
     *
     * @param operation the name of the invoked operation
     */
    public void removeInvocation(String operation) {
        if(operation == null || operation.isEmpty())
            activeInvocations.compute(DEFAULT_OPERATION, (key, count) -> internalCompute(count));
        else
            activeInvocations.compute(operation, (key, count) -> internalCompute(count));
    }

    /**
     * @param count the current count
     * @return the updated count
     */
    private Long internalCompute(Long count) {
        if (count == null) return null;
        if (count <= 1) return 0L;
        return count - 1L;
    }
}

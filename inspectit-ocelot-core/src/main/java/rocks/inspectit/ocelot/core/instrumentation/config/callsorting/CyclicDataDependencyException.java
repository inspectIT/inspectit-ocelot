package rocks.inspectit.ocelot.core.instrumentation.config.callsorting;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class CyclicDataDependencyException extends Exception {

    /**
     * A list of data keys representing the dependency cycle.
     */
    private List<String> dependencyCycle;
}

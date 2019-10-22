package rocks.inspectit.ocelot.config.model.instrumentation.actions;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

/**
 * Defines dependencies between different action calls ({@link ActionCallSettings}.
 * These dependencies are used to correctly order the action calls.
 */
@Data
@NoArgsConstructor
public class OrderSettings {

    /**
     * Allows to explicitly define which data keys this call reads before they are overridden by any other call.
     * These dependencies influence the execution order of calls,
     * e.g. calls specifying that they read data before it is overridden are executed
     * before any calls writing the data.
     * If a data key is present in both readsBeforeWritten and {@link #reads}, readsBeforeWritten takes precedence.
     */
    private Map<@NotBlank String, @NotNull Boolean> readsBeforeWritten = Collections.emptyMap();

    /**
     * Allows to explicitly define which data keys this call reads or does not read.
     * Implicitly, all data keys used as 'dataInput' or in the conditions are marked as "read".
     * These implicit dependencies can be removed by adding them with the value "false" to this map.
     * Additional dependencies can be added with the value "true".
     * These dependencies influence the execution order of calls, e.g. calls reading data are executed
     * after the calls writing the data.
     */
    private Map<@NotBlank String, @NotNull Boolean> reads = Collections.emptyMap();

    /**
     * Allows to explicitly define which data keys this call writes (e.g. due to side effects of the action).
     * These dependencies influence the execution order of calls,
     * e.g. calls specifying that they read data are executed
     * after any calls writing the data.
     */
    private Map<@NotBlank String, @NotNull Boolean> writes = Collections.emptyMap();

}

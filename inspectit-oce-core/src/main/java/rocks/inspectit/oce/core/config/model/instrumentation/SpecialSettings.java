package rocks.inspectit.oce.core.config.model.instrumentation;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settings for the {@link rocks.inspectit.oce.core.instrumentation.special.SpecialSensor}s.
 */
@Data
@NoArgsConstructor
public class SpecialSettings {

    /**
     * Enable or disables the @{@link rocks.inspectit.oce.core.instrumentation.special.ExecutorContextPropagationSensor}.
     */
    private boolean executorContextPropagation;
}

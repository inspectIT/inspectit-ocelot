package rocks.inspectit.ocelot.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.validation.constraints.AssertFalse;
import java.time.Duration;

/**
 * Defines the settings for the agent status.
 */
@Data
@NoArgsConstructor
public class AgentStatusSettings {

    /**
     * The period during which a non-ok and non-instrumentation-related status is valid.
     * Status changes due to instrumentation errors are valid until the next re-instrumentation.
     */
    @NonNull
    private Duration validityPeriod;

    @AssertFalse(message = "The specified period should not be negative!")
    public boolean isNegativeDuration() {
        return validityPeriod != null && validityPeriod.isNegative();
    }

}

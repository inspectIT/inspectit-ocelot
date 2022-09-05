package rocks.inspectit.ocelot.config.model.selfmonitoring;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import java.time.Duration;

/**
 * Defines the settings for the agent status.
 */
@Data
@NoArgsConstructor
public class AgentHealthSettings {

    /**
     * The period during which a non-ok and non-instrumentation-related status is valid.
     * Status changes due to instrumentation errors are valid until the next re-instrumentation.
     */
    @NonNull
    private Duration validityPeriod;

    /**
     * The minimum delay how often the AgentHealthManager checks for invalid agent health events to clear health status.
     */
    @NonNull
    private Duration minHealthCheckDelay;

    @AssertTrue(message = "minHealthCheckDelay must be at least 60 seconds")
    public boolean isMin60SecondsDelay() {
        return minHealthCheckDelay.toMinutes() >= 1;
    }

    @AssertFalse(message = "The specified period should not be negative!")
    public boolean isNegativeDuration() {
        return validityPeriod != null && validityPeriod.isNegative();
    }

}

package rocks.inspectit.ocelot.config.model.config;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class contains all settings that you can configure to retry an operation using exponential backoff.
 */
@Data
@NoArgsConstructor
public class RetrySettings {

    /** the maximum number of retry attempts. May not be lower than 1. */
    private int maxAttempts;

    /** the initial interval in milliseconds to wait before retrying. May not be lower than 1. */
    private long initialIntervalMillis;

    /** the value the current interval to wait for the next retry is multiplied with. May not be lower than 1.0. */
    private double multiplier;

    /**
     * factor to have variance in the calculated retry intervals. Must be greater than 0.0 and less than or equal 1.0.
     */
    private double randomizationFactor;
}

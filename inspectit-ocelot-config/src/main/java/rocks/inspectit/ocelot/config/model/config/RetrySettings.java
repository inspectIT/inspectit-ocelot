package rocks.inspectit.ocelot.config.model.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.time.DurationMin;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;

/**
 * This class contains all settings that you can configure to retry an operation using exponential backoff.
 */
@Data
@NoArgsConstructor
public class RetrySettings {
    /** true, if retries are enabled, false otherwise */
    private boolean enabled;

    /** the maximum number of retry attempts. May not be lower than 1. */
    @Min(1)
    private int maxAttempts;

    /** the initial interval in milliseconds to wait before retrying. May not be lower than 1. */
    @DurationMin(millis = 0, inclusive = false)
    private Duration initialInterval;

    /** the value the current interval to wait for the next retry is multiplied with. May not be lower than 1.0. */
    // We use a BigDecimal as there is no support for double in hibernate validator
    @DecimalMin("1.0")
    @NotNull
    private BigDecimal multiplier;

    /**
     * Random factor to have variance in the calculated retry intervals. Must be greater than 0.0 and less than or equal 1.0.
     */
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("1.0")
    @NotNull
    // We use a BigDecimal as there is no support for double in hibernate validator
    private BigDecimal randomizationFactor;

    /**
     * The maximum amount of time one retry is allowed to take. May not be lower than 1
     */
    @DurationMin(millis = 0, inclusive = false)
    private Duration timeLimit;
}

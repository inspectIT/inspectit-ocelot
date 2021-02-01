package rocks.inspectit.oce.eum.server.configuration.model;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Settings for exporting beacons via HTTP.
 */
@Data
@Validated
public class BeaconHttpExporterSettings {

    /**
     * Whether beacons should be exported via HTTP
     */
    @NotNull
    private boolean enabled;

    /**
     * The endpoint to which the beacons are to be sent
     */
    @NotBlank
    private String endpointUrl;

    /**
     * The max. amount of threads exporting beacons
     */
    @Min(1)
    private int workerThreads;

    /**
     * The maximum number of beacons to be exported using a single HTTP request
     */
    @Min(1)
    private int maxBatchSize;

    /**
     * The username used for Basic authentication
     */
    private String username;

    /**
     * The password used for Basic authentication
     */
    private String password;

    /**
     * The flush interval to export beacons in case the 'max-batch-size' has not been reached
     */
    @NotNull
    private Duration flushInterval;

    @AssertTrue(message = "Flush-Interval has to be greater or equal to 1 second.")
    public boolean isFlushIntervalGreaterThanOne() {
        return flushInterval.toMillis() >= 1000;
    }
}

package rocks.inspectit.ocelot.config.model.metrics.jmx;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;

/**
 * Settings for the JXM polling metrics recorder.
 */
@Data
@NoArgsConstructor
public class JmxMetricsRecorderSettings {

    /**
     * Master switch for enabling and disabling JMX metrics.
     */
    private boolean enabled;

    /**
     * Specifies the frequency in milliseconds with which the metrics should be polled and recorded.
     * Should default to ${inspectit.metrics.frequency}.
     */
    @NotNull
    private Duration frequency;

    /**
     * Force platform server creation before starting the metric pooling.
     */
    private boolean forcePlatformServer;

    /**
     * If the metric names should be lower-case..
     */
    private boolean lowerCaseMetricName;

    /**
     * Map of object names to white- or black-list. Keys are valid object name string representations.
     * <p>
     * Whitelisted are keys with value value <code>true</code>.
     * Blacklisted are keys with value value <code>false</code>.
     * <p>
     * Empty map means collect everything.
     *
     * @see javax.management.ObjectName
     */
    private Map<String, Boolean> objectNames;

}

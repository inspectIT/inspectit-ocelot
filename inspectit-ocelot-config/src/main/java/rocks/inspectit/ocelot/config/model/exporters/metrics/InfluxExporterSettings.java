package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;
import rocks.inspectit.ocelot.config.model.exporters.ExporterEnabledState;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Settings for the InfluxDB metrics exporter.
 */
@Data
public class InfluxExporterSettings {

    private static Logger LOGGER = Logger.getLogger(InfluxExporterSettings.class.getName());

    /**
     * Whether the exporter should be started.
     */
    private ExporterEnabledState enabled;

    @Deprecated
    /**
     * This property is deprecated since v2.0. Please use {@link #endpoint} instead.
     * The HTTP URL of influx (e.g. http://localhost:8086)
     */ private String url;

    /**
     * The HTTP URL endpoint of Influx (e.g., http://localhost:8086)
     */
    private String endpoint;

    /**
     * The database to which the values are pushed.
     */
    @NotBlank
    private String database;

    /**
     * The retention policy to use.
     */
    @NotBlank
    private String retentionPolicy;

    /**
     * The username to use for connecting to the influxDB, can be null.
     */
    private String user;

    /**
     * The password to use for connecting to the influxDB, can be null.
     */
    private String password;

    /**
     * Defines how often metrics are pushed to influx.
     */
    @DurationMin(millis = 1)
    private Duration exportInterval;

    /**
     * If enabled, the Influx Exporter creates the specified database upon initial connection.
     */
    private boolean createDatabase;

    /**
     * If disabled, the raw values of each counter will be written to the InfluxDB on each export.
     * When enabled, only the change of the counter in comparison to the previous export will be written.
     * This difference will only be exposed if the counter has changed (=the difference is non-zero).
     * This can greatly reduce the total data written to influx and makes writing queries easier.
     */
    private boolean countersAsDifferences;

    /**
     * The size of the buffer for failed batches.
     * E.g. if the exportInterval is 15s and the buffer-size is 4, the export will keep up to one minute of data in memory.
     */
    @Min(1)
    private int bufferSize;
}

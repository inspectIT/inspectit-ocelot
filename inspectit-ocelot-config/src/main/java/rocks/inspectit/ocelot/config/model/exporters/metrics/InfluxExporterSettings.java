package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Settings for the InfluxDB metrics exporter.
 */
@Data
public class InfluxExporterSettings {

    /**
     * If true, the influx exporter will be started (if the DB is not null)
     */
    private boolean enabled;

    /**
     * The HTTP URL of influx (e.g. http://localhost:8086)
     */
    private String url;

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

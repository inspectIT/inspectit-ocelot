package rocks.inspectit.ocelot.config.model.exporters.metrics;

import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;

import java.time.Duration;

@Data
public class InfluxExporterSettings {

    private boolean enabled;

    private String url;

    private String database;

    private String retentionPolicy;

    private String user;

    private String password;

    @DurationMin(millis = 1)
    private Duration exportInterval;

    private boolean createDatabase;
}

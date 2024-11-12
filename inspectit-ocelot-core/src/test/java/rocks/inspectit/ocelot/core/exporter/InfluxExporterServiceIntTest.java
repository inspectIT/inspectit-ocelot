package rocks.inspectit.ocelot.core.exporter;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opencensus.common.Scope;
import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
public class InfluxExporterServiceIntTest extends SpringTestBase {

    private String url;

    private static final String DATABASE = "ocelot_test";

    @RegisterExtension
    LogCapturer warnLogs = LogCapturer.create().captureForType(InfluxExporterService.class, org.slf4j.event.Level.WARN);

    @Container
    private final GenericContainer influx = new GenericContainer(DockerImageName.parse("influxdb:1.8"))
            .withExposedPorts(8086)
            .withEnv("INFLUXDB_DB", DATABASE)
            .withEnv("INFLUXDB_USER", "w00t")
            .withEnv("INFLUXDB_USER_PASSWORD", "password");

    @BeforeEach
    void startInfluxDB()  {
        String address = influx.getHost();
        Integer port = influx.getFirstMappedPort();
        this.url = "http://" + address + ":" + port;
    }

    @AfterEach
    void shutdownInfluxDB() {
       influx.stop();
    }

    private final String user = "w00t";

    private final String password = "password";

    @Test
    void verifyInfluxDataWritten() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.influx.enabled", true);
            props.setProperty("inspectit.exporters.metrics.influx.export-interval", "1s");
            props.setProperty("inspectit.exporters.metrics.influx.endpoint", url);
            props.setProperty("inspectit.exporters.metrics.influx.database", DATABASE);
            // note: user and password are mandatory as of v1.15.0
            props.setProperty("inspectit.exporters.metrics.influx.user", user);
            props.setProperty("inspectit.exporters.metrics.influx.password", password);
        });

        TagKey testTag = TagKey.create("my_tag");
        Measure.MeasureDouble testMeasure = Measure.MeasureDouble.create("my/test/measure", "foo", "bars");
        View testView = View.create(View.Name.create("my/test/measure/cool%data"), "", testMeasure, Aggregation.Sum.create(), Collections.singletonList(testTag));
        Stats.getViewManager().registerView(testView);

        try (Scope tc = Tags.getTagger().emptyBuilder().putLocal(testTag, TagValue.create("myval")).buildScoped()) {
            MeasureMap mm = Stats.getStatsRecorder().newMeasureMap();
            mm.put(testMeasure, 20.0);
            mm.record();

            mm = Stats.getStatsRecorder().newMeasureMap();
            mm.put(testMeasure, 22.0);
            mm.record();
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            try {
                InfluxDB iDB = InfluxDBFactory.connect(url, user, password); // note: user and password are mandatory as of v1.15.0
                QueryResult result = iDB.query(new Query("SELECT LAST(cool_data) FROM " + DATABASE + ".autogen.my_test_measure GROUP BY *"));

                List<QueryResult.Result> results = result.getResults();
                assertThat(results).hasSize(1);
                QueryResult.Result data = results.get(0);
                assertThat(data.getSeries()).hasSize(1);
                QueryResult.Series series = data.getSeries().get(0);
                assertThat(series.getTags()).hasSize(1).containsEntry("my_tag", "myval");
                assertThat(series.getValues().get(0).get(1)).isEqualTo(42.0);
            } catch (InfluxDBIOException exception) {
                // ignore
            }
        });
    }

    @DirtiesContext
    @Test
    void testNoEndpointSet() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.influx.endpoint", "");
            props.setProperty("inspectit.exporters.metrics.influx.enabled", "ENABLED");
        });
        warnLogs.assertContains("'endpoint'");
    }
}

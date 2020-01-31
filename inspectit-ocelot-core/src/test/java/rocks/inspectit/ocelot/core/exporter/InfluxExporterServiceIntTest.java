package rocks.inspectit.ocelot.core.exporter;

import com.bendb.influx.InfluxServer;
import com.bendb.influx.InfluxServerExtension;
import io.opencensus.common.Scope;
import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import rocks.inspectit.ocelot.core.SpringTestBase;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EnabledOnJre(JRE.JAVA_8) //because embedded influx uses some kotlin JRE8 classes which break otherwise
@ExtendWith(InfluxServerExtension.class)
public class InfluxExporterServiceIntTest extends SpringTestBase {

    InfluxServer influx;

    private static final String DATABASE = "ocelot_test";

    @Test
    void verifyInfluxDataWritten() {
        updateProperties(props -> {
            props.setProperty("inspectit.exporters.metrics.influx.export-interval", "1s");
            props.setProperty("inspectit.exporters.metrics.influx.url", influx.getUrl());
            props.setProperty("inspectit.exporters.metrics.influx.database", DATABASE);
        });

        TagKey testTag = TagKey.create("my_tag");
        Measure.MeasureDouble testMeasure = Measure.MeasureDouble.create("my/test/measure", "foo", "bars");
        View testView = View.create(View.Name.create("my/test/measure/cool%data"), "", testMeasure, Aggregation.Sum.create(), Arrays.asList(testTag));
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
            QueryResult result = InfluxDBFactory.connect(influx.getUrl()).query(new Query("SELECT LAST(cool_data) FROM " + DATABASE + ".autogen.my_test_measure GROUP BY *"));

            List<QueryResult.Result> results = result.getResults();
            assertThat(results).hasSize(1);
            QueryResult.Result data = results.get(0);
            assertThat(data.getSeries()).hasSize(1);
            QueryResult.Series series = data.getSeries().get(0);
            assertThat(series.getTags())
                    .hasSize(1)
                    .containsEntry("my_tag", "myval");
            assertThat(series.getValues().get(0).get(1)).isEqualTo(42.0);

        });
    }


}

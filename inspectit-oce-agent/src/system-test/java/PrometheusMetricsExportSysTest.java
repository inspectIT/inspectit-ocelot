import io.opencensus.stats.*;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusMetricsExportSysTest {

    private static final Tagger tagger = Tags.getTagger();
    private static final ViewManager viewManager = Stats.getViewManager();
    private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

    static CloseableHttpClient testClient;

    @BeforeAll
    static void initClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        testClient = builder.build();
    }

    @AfterAll
    static void closeClient() throws Exception {
        testClient.close();
    }

    @Test
    public void main() throws Exception {

        Measure.MeasureLong myMeasure = Measure.MeasureLong.create("prometheus_measure", "no actual meaning", "ms");
        View myView = View.create(View.Name.create("prometheus/view/test_measure"),
                "test view", myMeasure,
                Aggregation.Count.create(), Collections.emptyList());

        viewManager.registerView(myView);
        for (int i = 0; i < 42; i++) {
            statsRecorder.newMeasureMap().put(myMeasure, 7L).record();
        }

        try (CloseableHttpResponse response = testClient.execute(new HttpGet("http://localhost:8888/metrics"))) {
            String responseText = EntityUtils.toString(response.getEntity());
            assertThat(responseText).contains("prometheus_view_test_measure 42.0");
        }

    }
}

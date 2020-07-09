package rocks.inspectit.ocelot;

import io.opencensus.metrics.Metrics;
import io.opencensus.metrics.export.MetricProducerManager;
import io.opencensus.stats.*;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class UserMetricsCapturingSysTest {

    private static final ViewManager viewManager = Stats.getViewManager();

    private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

    @Test
    public void testUserMetricCapturing() {

        Measure.MeasureLong myMeasure = Measure.MeasureLong.create("test_measure", "no actual meaning", "ms");
        View myView = View.create(View.Name.create("test/view/test_measure"), "test view", myMeasure, Aggregation.Count.create(), Collections
                .emptyList());

        viewManager.registerView(myView);
        for (int i = 0; i < 42; i++) {
            statsRecorder.newMeasureMap().put(myMeasure, 7L).record();
        }

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        MetricProducerManager metricProducerManager = Metrics.getExportComponent().getMetricProducerManager();
        assertThat(metricProducerManager.getAllMetricProducer()).anyMatch(mp -> mp.getMetrics()
                .stream()
                .filter(m -> m.getMetricDescriptor().getName() == "test/view/test_measure")
                .count() == 1);
    }

}

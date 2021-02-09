package rocks.inspectit.oce.eum.server.metrics.percentiles;

import io.opencensus.common.Scope;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.export.Metric;
import io.opencensus.metrics.export.MetricDescriptor;
import io.opencensus.metrics.export.TimeSeries;
import io.opencensus.metrics.export.Value;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class TimeWindowViewManagerTest {

    private TimeWindowViewManager viewManager;

    private Supplier<Long> clock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void init() {
        clock = Mockito.mock(Supplier.class);
        lenient().doReturn(0L).when(clock).get();
        viewManager = new TimeWindowViewManager(clock);
        viewManager.init();
    }

    @AfterEach
    void destroy() {
        viewManager.destroy();
    }

    private void awaitMetricsProcessing() {
        await().until(() ->
                viewManager.worker.recordsQueue.isEmpty()
                        && viewManager.worker.worker.getState() == Thread.State.WAITING);
    }

    private void assertTotalSeriesCount(Collection<Metric> metrics, long expectedSeriesCount) {
        long count = metrics
                .stream()
                .flatMap(metric -> metric.getTimeSeriesList().stream())
                .count();
        assertThat(count).isEqualTo(expectedSeriesCount);
    }

    private void assertContainsMetric(Collection<Metric> metrics, String name, double value, String... tagKeyValuePairs) {
        assertThat(metrics)
                .anySatisfy(m -> assertThat(m.getMetricDescriptor().getName()).isEqualTo(name));
        Metric metric = metrics.stream()
                .filter(m -> m.getMetricDescriptor().getName().equals(name))
                .findFirst()
                .get();

        List<LabelKey> keys = metric.getMetricDescriptor().getLabelKeys();
        assertThat(keys).hasSize(tagKeyValuePairs.length / 2);
        List<LabelValue> values = new ArrayList<>();
        keys.forEach(label -> values.add(null));

        for (int i = 0; i < tagKeyValuePairs.length; i += 2) {
            LabelKey tagKey = LabelKey.create(tagKeyValuePairs[i], "");
            LabelValue tagValue = LabelValue.create(tagKeyValuePairs[i + 1]);
            assertThat(keys).contains(tagKey);
            values.set(keys.indexOf(tagKey), tagValue);
        }

        assertThat(metric.getTimeSeriesList())
                .anySatisfy(ts -> assertThat(ts.getLabelValues()).isEqualTo(values));
        TimeSeries ts = metric.getTimeSeriesList().stream()
                .filter(series -> series.getLabelValues().equals(values))
                .findFirst().get();

        assertThat(ts.getPoints()).hasSize(1);
        assertThat(ts.getPoints().get(0).getValue()).isEqualTo(Value.doubleValue(value));

    }

    @Nested
    class GetMeasureForSeries {

        @Test
        void noViewsRegistered() {
            assertThat(viewManager.getMeasureNameForSeries("test")).isNull();
        }

        @Test
        void singleViewRegisteredPercentiles() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList(), 1);

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_smoothed_average")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();
        }

        @Test
        void singleViewRegisteredSmoothedAverage() {
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/view_smoothed_average", "ms", "foo",
                    0.05, 0.05, 15000, Collections.emptyList(), 1);

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_smoothed_average")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isNull();
        }

        @Test
        void multipleViewsRegistered() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList(), 1);

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();

            viewManager.createOrUpdatePercentileView("my/other_measure", "my/other_view", "ms", "foo",
                    false, true, Collections.emptySet(), 15000, Collections.emptyList(), 1);

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_max")).isEqualTo("my/other_measure");

            viewManager.createOrUpdateSmoothedAverageView("my/further_measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.05, 0.1, 15000, Collections.emptyList(), 1);

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_smoothed_average")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_smoothed_average")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_max")).isEqualTo("my/other_measure");
            assertThat(viewManager.getMeasureNameForSeries("my/further_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_smoothed_average")).isEqualTo("my/further_measure");
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_max")).isNull();
        }

        @Test
        void viewRemoved() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList(), 1);
            viewManager.createOrUpdatePercentileView("my/other_measure", "my/other_view", "ms", "foo",
                    false, true, Collections.emptySet(), 15000, Collections.emptyList(), 1);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.07, 0.5, 15000, Collections.emptyList(), 1);

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_smoothed_average")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_smoothed_average")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_max")).isEqualTo("my/other_measure");
            assertThat(viewManager.getMeasureNameForSeries("my/further_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_smoothed_average")).isEqualTo("my/measure");
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_max")).isNull();

            viewManager.removeView("my/measure", "my/view");
            viewManager.removeView("my/measure", "my/further_view_smoothed_average");

            assertThat(viewManager.getMeasureNameForSeries("my/view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/view_max")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/other_view_max")).isEqualTo("my/other_measure");
            assertThat(viewManager.getMeasureNameForSeries("my/further_view")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_smoothed_average")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_min")).isNull();
            assertThat(viewManager.getMeasureNameForSeries("my/further_view_max")).isNull();
        }
    }

    @Nested
    class ComputeMetrics {

        @Test
        void testNoData() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList(), 1);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.07, 0.5, 15000, Collections.emptyList(), 1);

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(4);
            assertTotalSeriesCount(result, 0);
        }


        @Test
        void testWithData() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList(), 100);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.2, 0.2, 15000, Collections.emptyList(), 100);

            for (int i = 1; i < 100; i++) {
                doReturn((long) i).when(clock).get();
                viewManager.recordMeasurement("my/measure", i);
            }
            awaitMetricsProcessing();

            doReturn(10000L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(4);
            assertTotalSeriesCount(result, 5);
            assertContainsMetric(result, "my/view_min", 1);
            assertContainsMetric(result, "my/view_max", 99);
            assertContainsMetric(result, "my/further_view_smoothed_average", 50.0);
            assertContainsMetric(result, "my/view", 50, "quantile", "0.5");
            assertContainsMetric(result, "my/view", 95, "quantile", "0.95");
        }

        @Test
        void testMultiSeriesData() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Arrays.asList("tag1", "tag2"), 198);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.2, 0.2, 15000, Arrays.asList("tag1", "tag2"), 198);

            for (int i = 1; i < 100; i++) {
                doReturn((long) i).when(clock).get();
                viewManager.recordMeasurement("my/measure", i);
                try (Scope s = Tags.getTagger().emptyBuilder()
                        .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                        .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                    viewManager.recordMeasurement("my/measure", 1000 + i);
                }
            }
            awaitMetricsProcessing();

            doReturn(10000L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(4);
            assertTotalSeriesCount(result, 10);
            assertContainsMetric(result, "my/view_min", 1, "tag1", "", "tag2", "");
            assertContainsMetric(result, "my/view_max", 99, "tag1", "", "tag2", "");
            assertContainsMetric(result, "my/further_view_smoothed_average", 50.0, "tag1", "", "tag2", "");
            assertContainsMetric(result, "my/view", 50, "tag1", "", "tag2", "", "quantile", "0.5");
            assertContainsMetric(result, "my/view", 95, "tag1", "", "tag2", "", "quantile", "0.95");
            assertContainsMetric(result, "my/view_min", 1001, "tag1", "foo", "tag2", "bar");
            assertContainsMetric(result, "my/view_max", 1099, "tag1", "foo", "tag2", "bar");
            assertContainsMetric(result, "my/further_view_smoothed_average", 1050.0, "tag1", "foo", "tag2", "bar");
            assertContainsMetric(result, "my/view", 1050, "tag1", "foo", "tag2", "bar", "quantile", "0.5");
            assertContainsMetric(result, "my/view", 1095, "tag1", "foo", "tag2", "bar", "quantile", "0.95");
        }

        @Test
        void testMultipleViewsForSameMeasure() {
            viewManager.createOrUpdatePercentileView("my/measure", "viewA", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 1);
            viewManager.createOrUpdatePercentileView("my/measure", "viewB", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag1"), 1);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "viewC_smoothed_average", "ms", "foo",
                    0.15, 0.45, 1, Arrays.asList("tag2"), 1);

            viewManager.recordMeasurement("my/measure", 1);
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(3);
            assertTotalSeriesCount(result, 3);
            assertContainsMetric(result, "viewA_min", 1);
            assertContainsMetric(result, "viewB_min", 1, "tag1", "");
            assertContainsMetric(result, "viewC_smoothed_average", 1, "tag2", "");
        }

        @Test
        void testWithStaleData() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, true, Arrays.asList(0.5, 0.95), 15000, Collections.emptyList(), 99);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.05, 0.05, 15000, Collections.emptyList(), 99);

            for (int i = 1; i < 100; i++) {
                doReturn((long) i).when(clock).get();
                viewManager.recordMeasurement("my/measure", i);
            }
            awaitMetricsProcessing();
            doReturn(20000L).when(clock).get();

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(4);
            assertTotalSeriesCount(result, 0);
        }

        @Test
        void testDroppingBecauseBufferIsFull() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag"), 10);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.05, 0.05, 1, Arrays.asList("tag"), 10);

            doReturn(0L).when(clock).get();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag"), TagValue.create("foo"))
                    .buildScoped()) {
                for (int i = 0; i < 20; i++) {
                    viewManager.recordMeasurement("my/measure", 20 - i);
                }
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(2);
            assertTotalSeriesCount(result, 2);
            assertContainsMetric(result, "my/view_min", 11.0, "tag", "foo");
            assertContainsMetric(result, "my/further_view_smoothed_average", 15.5, "tag", "foo");
        }

        @Test
        void testDroppingPreventedThroughCleanupTask() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag"), 10);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.05, 0.05, 1, Arrays.asList("tag"), 10);

            doReturn(0L).when(clock).get();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag"), TagValue.create("foo"))
                    .buildScoped()) {
                for (int i = 0; i < 10; i++) {
                    viewManager.recordMeasurement("my/measure", i);
                }
            }
            awaitMetricsProcessing();
            doReturn(10000L).when(clock).get();
            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag"), TagValue.create("bar"))
                    .buildScoped()) {
                viewManager.recordMeasurement("my/measure", 1000);
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();

            assertThat(result).hasSize(2);
            assertTotalSeriesCount(result, 2);
            assertContainsMetric(result, "my/view_min", 1000, "tag", "bar");
            assertContainsMetric(result, "my/further_view_smoothed_average", 1000, "tag", "bar");
        }
    }

    @Nested
    class CreateOrUpdateView {

        @Test
        void updateMetricDescription() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "s", "bar",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            MetricDescriptor md = result.iterator().next().getMetricDescriptor();

            assertThat(md.getName()).isEqualTo("my/view_min");
            assertThat(md.getUnit()).isEqualTo("s");
            assertThat(md.getDescription()).isEqualTo("bar");
        }

        @Test
        void updateMinMaxPercentiles() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "s", "bar",
                    false, true, Arrays.asList(0.5), 1, Collections.emptyList(), 100);

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(2);
            assertContainsMetric(result, "my/view_max", 42);
            assertContainsMetric(result, "my/view", 42, "quantile", "0.5");
        }

        @Test
        void updateSmoothedAverage() {
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/view_smoothed_average", "ms", "foo",
                    0.2, 0.2, 1, Collections.emptyList(), 100);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/view_smoothed_average", "s", "bar",
                    0.0, 0.05, 1, Arrays.asList("tag1", "tag2"), 100);

            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                    .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                viewManager.recordMeasurement("my/measure", 42);
                viewManager.recordMeasurement("my/measure", 43);
                viewManager.recordMeasurement("my/measure", 44);
                viewManager.recordMeasurement("my/measure", 45);
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view_smoothed_average", 44, "tag1", "foo", "tag2", "bar");
        }

        @Test
        void updateTimeWindow() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 100, Collections.emptyList(), 100);

            doReturn(0L).when(clock).get();
            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            doReturn(99L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view_min", 42);
        }

        @Test
        void updateBufferSize() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 100, Collections.emptyList(), 100);
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 100, Collections.emptyList(), 1);

            doReturn(0L).when(clock).get();
            viewManager.recordMeasurement("my/measure", 100);
            viewManager.recordMeasurement("my/measure", 10);
            awaitMetricsProcessing();

            doReturn(99L).when(clock).get();
            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view_min", 100); //because the second point has been dropped
        }

        @Test
        void updateTags() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag1", "tag2"), 100);

            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                    .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                viewManager.recordMeasurement("my/measure", 42);
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view_min", 42, "tag1", "foo", "tag2", "bar");
        }

        @Test
        void updateWithValueRecorded() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.2, 0.2, 1, Collections.emptyList(), 100);

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Arrays.asList("tag1", "tag2"), 100);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "s", "bar",
                    0.0, 0.05, 1, Arrays.asList("tag1", "tag2"), 100);

            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                    .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                viewManager.recordMeasurement("my/measure", 42);
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(2);
            assertContainsMetric(result, "my/view_min", 42, "tag1", "foo", "tag2", "bar");
            assertContainsMetric(result, "my/further_view_smoothed_average", 42, "tag1", "foo", "tag2", "bar");
        }

        @Test
        void updatePercentileToSmoothedAverage() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 1, Collections.emptyList(), 100);

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/view", "s", "bar",
                    0.0, 0.05, 1, Arrays.asList("tag1", "tag2"), 100);

            try (Scope s = Tags.getTagger().emptyBuilder()
                    .putLocal(TagKey.create("tag1"), TagValue.create("foo"))
                    .putLocal(TagKey.create("tag2"), TagValue.create("bar")).buildScoped()) {
                viewManager.recordMeasurement("my/measure", 42);
            }
            awaitMetricsProcessing();

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(result).hasSize(1);
            assertContainsMetric(result, "my/view", 42, "tag1", "foo", "tag2", "bar");
        }
    }

    @Nested
    class RemoveView {

        @Test
        void checkViewRemoved() {
            viewManager.createOrUpdatePercentileView("my/measure", "my/view", "ms", "foo",
                    true, false, Collections.emptyList(), 100, Collections.emptyList(), 100);
            viewManager.createOrUpdateSmoothedAverageView("my/measure", "my/further_view_smoothed_average", "ms", "foo",
                    0.2, 0.2, 1, Collections.emptyList(), 100);

            viewManager.recordMeasurement("my/measure", 42);
            awaitMetricsProcessing();

            boolean removed = viewManager.removeView("my/measure", "my/view");
            boolean removed_smoothed_average = viewManager.removeView("my/measure", "my/further_view_smoothed_average");

            Collection<Metric> result = viewManager.computeMetrics();
            assertThat(viewManager.areAnyViewsRegisteredForMeasure("my/measure")).isFalse();
            assertThat(removed).isTrue();
            assertThat(removed_smoothed_average).isTrue();
            assertThat(result).isEmpty();

        }

        @Test
        void removeNonExistingView() {
            boolean removed = viewManager.removeView("my/measure", "my/view");
            assertThat(removed).isFalse();
        }

    }
}

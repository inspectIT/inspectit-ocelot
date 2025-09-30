package rocks.inspectit.ocelot.core.metrics.timewindow.worker;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.core.metrics.timewindow.TimeWindowViewManager;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.QuantilesView;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.SmoothedAverageView;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.TimeWindowView;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static rocks.inspectit.ocelot.core.metrics.timewindow.utils.TimeWindowTestUtils.assertContainsData;
import static rocks.inspectit.ocelot.core.metrics.timewindow.utils.TimeWindowTestUtils.assertTotalSeriesCount;

@ExtendWith(MockitoExtension.class)
class TimeWindowMetricProducerTest {

    @Mock
    TimeWindowViewManager viewManager;

    @InjectMocks
    TimeWindowMetricProducer producer;

    @InjectMocks
    TimeWindowRecorder recorder;

    static final String metricName = "test";

    static final String quantileViewName = "test/quantiles";

    static final String smoothedAvgViewName = "test/smoothed/average";

    @BeforeEach
    void createViews() {
        TimeWindowView quantilesView = createQuantilesView(1000);
        TimeWindowView smoothedAvgView = createSmoothedAverageView(1000);
        List<TimeWindowView> views = Arrays.asList(quantilesView, smoothedAvgView);
        lenient().when(viewManager.areAnyViewsRegistered(metricName)).thenReturn(true);
        lenient().when(viewManager.getAllViews()).thenReturn(views);
        lenient().when(viewManager.getViews(metricName)).thenReturn(views);
    }

    @Test
    void shouldReturnEmptySeriesWhenNoDataRecorded() {
        Collection<MetricData> result = producer.produce(Resource.empty());

        assertThat(result).hasSize(4);
        assertTotalSeriesCount(result, 0);
    }

    @Test
    void shouldReturnSeriesWhenDataRecorded() {
        for (int i = 1; i < 100; i++) {
            recorder.recordMetric(metricName, i, Baggage.empty());
        }
        awaitMetricsProcessing();

        Collection<MetricData> result =  producer.produce(Resource.empty());

        assertThat(result).hasSize(4);
        assertTotalSeriesCount(result, 5);
        assertContainsData(result, quantileViewName, 50, ImmutableMap.of("quantile", "0.5"));
        assertContainsData(result, quantileViewName, 95,  ImmutableMap.of("quantile", "0.95"));
        assertContainsData(result, quantileViewName + "_max", 99, emptyMap());
        assertContainsData(result, quantileViewName + "_min", 1, emptyMap());
        assertContainsData(result, smoothedAvgViewName, 50, emptyMap());
    }

    @Test
    void shouldReturnMultipleSeriesWhenDataRecorded() {
        Baggage baggage = Baggage.builder()
                .put("key1", "foo")
                .put("key2", "bar")
                .build();

        for (int i = 1; i < 100; i++) {
            recorder.recordMetric(metricName, i, Baggage.empty());
            recorder.recordMetric(metricName, 1000+i, baggage);
        }
        awaitMetricsProcessing();

        Collection<MetricData> result =  producer.produce(Resource.empty());

        assertThat(result).hasSize(4);
        assertTotalSeriesCount(result, 10);

        assertContainsData(result, quantileViewName, 50, ImmutableMap.of("quantile", "0.5"));
        assertContainsData(result, quantileViewName, 95, ImmutableMap.of("quantile", "0.95"));
        assertContainsData(result, quantileViewName, 1050, ImmutableMap.of("key1", "foo", "key2", "bar", "quantile", "0.5"));
        assertContainsData(result, quantileViewName, 1095, ImmutableMap.of("key1", "foo", "key2", "bar", "quantile", "0.95"));
        assertContainsData(result, quantileViewName + "_min", 1, emptyMap());
        assertContainsData(result, quantileViewName + "_min", 1001, ImmutableMap.of("key1", "foo", "key2", "bar"));
        assertContainsData(result, quantileViewName + "_max", 99, emptyMap());
        assertContainsData(result, quantileViewName + "_max", 1099, ImmutableMap.of("key1", "foo", "key2", "bar"));
        assertContainsData(result, smoothedAvgViewName, 50, emptyMap());
        assertContainsData(result, smoothedAvgViewName, 1050, ImmutableMap.of("key1", "foo", "key2", "bar"));
    }


    @Test
    void shouldReturnEmptySeriesWhenStaleData() throws Exception{
        for (int i = 1; i < 100; i++) {
            recorder.recordMetric(metricName, i, Baggage.empty());
        }
        awaitMetricsProcessing();
        Thread.sleep(300); // wait longer than timeWindow

        Collection<MetricData> result =  producer.produce(Resource.empty());

        assertThat(result).hasSize(4);
        assertTotalSeriesCount(result, 0);
    }

    @Test
    void testDroppingBecauseBufferIsFull() {
        TimeWindowView quantilesView = createQuantilesView(10);
        TimeWindowView smoothedAvgView = createSmoothedAverageView(10);
        List<TimeWindowView> views = Arrays.asList(quantilesView, smoothedAvgView);
        when(viewManager.getAllViews()).thenReturn(views);
        when(viewManager.getViews(metricName)).thenReturn(views);

        Baggage baggage = Baggage.builder().put("key1", "foo").build();

        for (int i = 0; i < 20; i++) {
            recorder.recordMetric(metricName, 20-i, baggage);
        }
        awaitMetricsProcessing();

        Collection<MetricData> result =  producer.produce(Resource.empty());

        assertThat(result).hasSize(4);
        assertTotalSeriesCount(result, 5);
        assertContainsData(result, quantileViewName + "_min", 11.0, ImmutableMap.of("key1", "foo"));
        assertContainsData(result, smoothedAvgViewName, 15.5, ImmutableMap.of("key1", "foo"));
    }

    @Test
    void testDroppingPreventedThroughCleanupTask() throws Exception {
        TimeWindowView quantilesView = createQuantilesView(10);
        TimeWindowView smoothedAvgView = createSmoothedAverageView(10);
        List<TimeWindowView> views = Arrays.asList(quantilesView, smoothedAvgView);
        when(viewManager.getAllViews()).thenReturn(views);
        when(viewManager.getViews(metricName)).thenReturn(views);

        Baggage baggage = Baggage.builder().put("key1", "foo").build();

        // fill buffer
        for (int i = 0; i < 10; i++) {
            recorder.recordMetric(metricName, i, baggage);
        }
        awaitMetricsProcessing();
        Thread.sleep(300); // wait longer than timeWindow

        // clean-up task should clean the full buffer, so we can record new values again
        recorder.recordMetric(metricName, 1000, baggage);
        awaitMetricsProcessing();

        Collection<MetricData> result = producer.produce(Resource.empty());

        assertThat(result).hasSize(4);
        assertTotalSeriesCount(result, 5);
        assertContainsData(result, quantileViewName + "_min", 1000, ImmutableMap.of("key1", "foo"));
        assertContainsData(result, smoothedAvgViewName, 1000, ImmutableMap.of("key1", "foo"));
    }

    @Test
    void shouldCacheResultMetricsWithinCacheDuration() {
        Collection<MetricData> result1 = producer.produce(Resource.empty());

        Collection<MetricData> result2 = producer.produce(Resource.empty());

        assertThat(result1).isSameAs(result2);
    }

    @Test
    void shouldCreateNewResultMetricsWhenCacheDurationExceeded() throws Exception {
        Collection<MetricData> result1 = producer.produce(Resource.empty());

        Thread.sleep(1100); // wait longer than cache duration

        Collection<MetricData> result2 = producer.produce(Resource.empty());

        assertThat(result1).isNotSameAs(result2);
    }

    static TimeWindowView createQuantilesView(int bufferLimit) {
        return new QuantilesView(quantileViewName, "desc", "ms", ImmutableSet.of("key1", "key2"),
                Duration.ofMillis(200), bufferLimit, ImmutableSet.of(0.5, 0.95), true, true);
    }

    static TimeWindowView createSmoothedAverageView(int bufferLimit) {
        return new SmoothedAverageView(smoothedAvgViewName, "desc", "ms",
                ImmutableSet.of("key1", "key2"), Duration.ofMillis(200), bufferLimit, 0.2, 0.2);
    }

    /**
     * Processes all metrics records until the queue is empty
     */
    void awaitMetricsProcessing() {
        do {
            recorder.record();
        } while(!recorder.recordsQueue.isEmpty());
    }
}

package rocks.inspectit.ocelot.core.metrics.timewindow.views;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static rocks.inspectit.ocelot.core.metrics.timewindow.utils.TimeWindowTestUtils.assertContainsData;

class SmoothedAverageViewTest {

    @Nested
    class Constructor {

        @Test
        void invalidDropUpper() {
            assertThatThrownBy(() -> new SmoothedAverageView("name", "description", "unit",
                    Collections.emptySet(), Duration.ofSeconds(1), 1000, -1.0, 0.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidDropLower() {
            assertThatThrownBy(() -> new SmoothedAverageView("name", "description", "unit",
                    Collections.emptySet(), Duration.ofSeconds(1), 1000, 0.05, 1.01))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ComputeMetrics {

        final String name = "name";

        final String desc = "description";

        final String unit = "unit";

        @Test
        void checkSmoothedAverageMetricData() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, Collections.emptySet(),
                    Duration.ofMillis(10), 1, 0.0, 0.05);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertThat(result).hasSize(1);
            Optional<MetricData> maybeMetric = result.stream().findFirst();

            assertThat(maybeMetric).isNotEmpty();
            MetricData metric = maybeMetric.get();

            assertThat(metric.getName()).isEqualTo(name);
            assertThat(metric.getDescription()).isEqualTo(desc);
            assertThat(metric.getUnit()).isEqualTo(unit);
            assertThat(metric.getType()).isEqualTo(MetricDataType.DOUBLE_GAUGE);
        }

        @Test
        void checkSmoothedAveragePointData() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 12, 0.05, 0.05);

            Baggage baggage1 = Baggage.builder().put("my-attr", "foo").build();
            Baggage baggage2 = Baggage.builder().put("my-attr", "bar").build();
            Instant timestamp = Instant.now();

            view.insertValue(42, timestamp, baggage1);
            view.insertValue(99, timestamp.plus(1, ChronoUnit.MILLIS), baggage1);
            view.insertValue(101, timestamp.plus(1, ChronoUnit.MILLIS), baggage1);
            view.insertValue(50, timestamp.plus(1, ChronoUnit.MILLIS), baggage1);
            view.insertValue(68, timestamp.plus(1, ChronoUnit.MILLIS), baggage1);
            view.insertValue(70, timestamp.plus(2, ChronoUnit.MILLIS), baggage1);

            view.insertValue(101, timestamp.plus(2, ChronoUnit.MILLIS), baggage2);
            view.insertValue(150, timestamp.plus(3, ChronoUnit.MILLIS), baggage2);
            view.insertValue(171, timestamp.plus(3, ChronoUnit.MILLIS), baggage2);
            view.insertValue(250, timestamp.plus(4, ChronoUnit.MILLIS), baggage2);
            view.insertValue(99, timestamp.plus(4, ChronoUnit.MILLIS), baggage2);
            view.insertValue(101, timestamp.plus(5, ChronoUnit.MILLIS), baggage2);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 71.75, ImmutableMap.of("my-attr", "foo"));
            assertContainsData(result, name, 130.75, ImmutableMap.of("my-attr", "bar"));
        }

        @Test
        void checkSmoothedAverageMetricGreatIndex() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 24, 0.2, 0.2);

            insertValues(view);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 97.14285714285714, ImmutableMap.of("my-attr", "foo"));
        }

        @Test
        void checkSmoothedAverageMetricDropOnlyLower() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 24, 0.0, 0.2);

            insertValues(view);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 123.78947368421052, ImmutableMap.of("my-attr", "foo"));
        }

        @Test
        void checkSmoothedAverageMetricDropOnlyUpper() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 24, 0.11, 0.0);

            insertValues(view);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 92.04761904761905, ImmutableMap.of("my-attr", "foo"));
        }

        @Test
        void checkSmoothedAverageMetricDropGreaterUpperIndex() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 24, 0.9, 0.2);

            insertValues(view);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 68.0, ImmutableMap.of("my-attr", "foo"));
        }

        @Test
        void checkSmoothedAverageMetricDropNothing() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 4, 0.0, 0.0);

            Baggage baggage = Baggage.builder().put("my-attr", "foo").build();
            Instant timestamp = Instant.now();

            view.insertValue(80, timestamp, baggage);
            view.insertValue(87, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
            view.insertValue(100, timestamp.plus(2, ChronoUnit.MILLIS), baggage);
            view.insertValue(150, timestamp.plus(2, ChronoUnit.MILLIS), baggage);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 104.25, ImmutableMap.of("my-attr", "foo"));
        }

        @Test
        void checkSmoothedAverageMetricDropSmallValues() {
            TimeWindowView view = new SmoothedAverageView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 4, 0.1, 0.0);

            Baggage baggage = Baggage.builder().put("my-attr", "foo").build();
            Instant timestamp = Instant.now();

            view.insertValue(116, timestamp, baggage);
            view.insertValue(125, timestamp.plus(1, ChronoUnit.MILLIS), baggage);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 116, ImmutableMap.of("my-attr", "foo"));
        }
    }

    /**
     * Helper method to insert values into the view
     */
    private static void insertValues(TimeWindowView view) {
        Baggage baggage = Baggage.builder().put("my-attr", "foo").build();
        Instant timestamp = Instant.now();

        view.insertValue(42, timestamp, baggage);
        view.insertValue(99, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(101, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(50, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(68,timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(70, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(42, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(99, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(101, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(50, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(68, timestamp.plus(1, ChronoUnit.MILLIS), baggage);
        view.insertValue(70, timestamp.plus(2, ChronoUnit.MILLIS), baggage);
        view.insertValue(101, timestamp.plus(2, ChronoUnit.MILLIS), baggage);
        view.insertValue(150, timestamp.plus(3, ChronoUnit.MILLIS), baggage);
        view.insertValue(171, timestamp.plus(3, ChronoUnit.MILLIS), baggage);
        view.insertValue(250, timestamp.plus(4, ChronoUnit.MILLIS), baggage);
        view.insertValue(99, timestamp.plus(4, ChronoUnit.MILLIS), baggage);
        view.insertValue(101, timestamp.plus(5, ChronoUnit.MILLIS), baggage);
        view.insertValue(101, timestamp.plus(5, ChronoUnit.MILLIS), baggage);
        view.insertValue(150, timestamp.plus(5, ChronoUnit.MILLIS), baggage);
        view.insertValue(171, timestamp.plus(6, ChronoUnit.MILLIS), baggage);
        view.insertValue(250, timestamp.plus(6, ChronoUnit.MILLIS), baggage);
        view.insertValue(99, timestamp.plus(6, ChronoUnit.MILLIS), baggage);
        view.insertValue(101, timestamp.plus(7, ChronoUnit.MILLIS), baggage);
    }
}

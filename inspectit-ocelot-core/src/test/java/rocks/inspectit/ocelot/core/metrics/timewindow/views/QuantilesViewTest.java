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

class QuantilesViewTest {

    final String name = "name";

    final String desc = "description";

    final String unit = "unit";

    @Nested
    class Constructor {

        @Test
        void noQuantilesAndMinMaxSpecified() {
            assertThatThrownBy(() -> new QuantilesView(name, desc, unit, Collections.emptySet(),
                    Duration.ofSeconds(1), 1000, Collections.emptySet(), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidQuantile() {
            assertThatThrownBy(() -> new QuantilesView(name, desc, unit, Collections.emptySet(),
                    Duration.ofSeconds(1), 1000, ImmutableSet.of(1.0), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankName() {
            assertThatThrownBy(() -> new QuantilesView(" ", desc, unit, Collections.emptySet(),
                    Duration.ofSeconds(1), 1000, Collections.emptySet(), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankDescription() {
            assertThatThrownBy(() -> new QuantilesView(name, " ", unit, Collections.emptySet(),
                    Duration.ofSeconds(1), 1000, Collections.emptySet(), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankUnit() {
            assertThatThrownBy(() -> new QuantilesView(name, desc, " ", Collections.emptySet(),
                    Duration.ofSeconds(1), 1000, Collections.emptySet(), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidTimeWindow() {
            assertThatThrownBy(() -> new QuantilesView(name, desc, unit, Collections.emptySet(),
                    Duration.ZERO, 1000, Collections.emptySet(), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidBufferSize() {
            assertThatThrownBy(() -> new QuantilesView(name, desc, " ", Collections.emptySet(),
                    Duration.ofSeconds(1), 0, Collections.emptySet(), false, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    class GetQuantileAttribute {

        @Test
        void unnecessaryZeroesOmitted() {
            String attr = QuantilesView.getQuantileAttribute(0.50);
            assertThat(attr).isEqualTo("0.5");
        }

        @Test
        void tooLongValueRoundedDown() {
            String attr = QuantilesView.getQuantileAttribute(1.0 / 3);
            assertThat(attr).isEqualTo("0.33333");
        }

        @Test
        void tooLongValueRoundedUp() {
            String attr = QuantilesView.getQuantileAttribute(1.0 / 3 * 2);
            assertThat(attr).isEqualTo("0.66667");
        }
    }

    @Nested
    class ComputeMetrics {

        @Test
        void checkQuantileMetricData() {
            TimeWindowView view = new QuantilesView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 1, ImmutableSet.of(0.5), false, false);

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
        void checkMinMetricData() {
            TimeWindowView view = new QuantilesView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 1, Collections.emptySet(), false, true);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertThat(result).hasSize(1);
            Optional<MetricData> maybeMetric = result.stream().findFirst();

            assertThat(maybeMetric).isNotEmpty();
            MetricData metric = maybeMetric.get();

            assertThat(metric.getName()).isEqualTo(name + "_min");
            assertThat(metric.getDescription()).isEqualTo(desc);
            assertThat(metric.getUnit()).isEqualTo(unit);
            assertThat(metric.getType()).isEqualTo(MetricDataType.DOUBLE_GAUGE);
        }

        @Test
        void checkMaxMetricData() {
            TimeWindowView view = new QuantilesView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 1, Collections.emptySet(), true, false);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertThat(result).hasSize(1);
            Optional<MetricData> maybeMetric = result.stream().findFirst();

            assertThat(maybeMetric).isNotEmpty();
            MetricData metric = maybeMetric.get();

            assertThat(metric.getName()).isEqualTo(name + "_max");
            assertThat(metric.getDescription()).isEqualTo(desc);
            assertThat(metric.getUnit()).isEqualTo(unit);
            assertThat(metric.getType()).isEqualTo(MetricDataType.DOUBLE_GAUGE);
        }

        @Test
        void checkMinimumMetric() {
            TimeWindowView view = new QuantilesView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 4, Collections.emptySet(), false, true);

            insertValues(view);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name + "_min", 42, ImmutableMap.of("my-attr", "foo"));
            assertContainsData(result, name + "_min", 100, ImmutableMap.of("my-attr", "bar"));
        }

        @Test
        void checkMaximumMetric() {
            TimeWindowView view = new QuantilesView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 4, Collections.emptySet(), true, false);

            insertValues(view);

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name + "_max", 99, ImmutableMap.of("my-attr", "foo"));
            assertContainsData(result, name + "_max", 101, ImmutableMap.of("my-attr", "bar"));
        }

        @Test
        void checkQuantileMetrics() {
            TimeWindowView view = new QuantilesView(name, desc, unit, ImmutableSet.of("my-attr"),
                    Duration.ofMillis(10), 18, ImmutableSet.of(0.5, 0.9), false, false);

            Baggage baggage1 = Baggage.builder().put("my-attr", "foo").build();
            Baggage baggage2 = Baggage.builder().put("my-attr", "bar").build();

            for (int i = 1; i < 10; i++) {
                Instant timestamp = Instant.now();
                view.insertValue(10 + i, timestamp, baggage1);
                view.insertValue(100 + i, timestamp.plus(2, ChronoUnit.MILLIS), baggage2);
            }

            Collection<MetricData> result = view.computeMetrics(Instant.now(), Resource.empty());

            assertContainsData(result, name, 19, ImmutableMap.of("my-attr", "foo", "quantile", "0.9"));
            assertContainsData(result, name, 109, ImmutableMap.of("my-attr", "bar", "quantile", "0.9"));
            assertContainsData(result, name, 15, ImmutableMap.of("my-attr", "foo", "quantile", "0.5"));
            assertContainsData(result, name, 105, ImmutableMap.of("my-attr", "bar", "quantile", "0.5"));
        }
    }

    /**
     * Helper method to insert values into the view
     */
    static void insertValues(TimeWindowView view) {
        Baggage baggage1 = Baggage.builder().put("my-attr", "foo").build();
        Baggage baggage2 = Baggage.builder().put("my-attr", "bar").build();
        Instant timestamp = Instant.now();

        view.insertValue(42, timestamp, baggage1);
        view.insertValue(99, timestamp.plus(1, ChronoUnit.MILLIS), baggage1);
        view.insertValue(101, timestamp.plus(2, ChronoUnit.MILLIS), baggage2);
        view.insertValue(100, timestamp.plus(3, ChronoUnit.MILLIS), baggage2);
    }
}

package rocks.inspectit.oce.eum.server.metrics.percentiles;

import com.google.common.collect.ImmutableSet;
import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.export.Metric;
import io.opencensus.metrics.export.MetricDescriptor;
import io.opencensus.metrics.export.Value;
import io.opencensus.tags.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SmoothedAverageViewTest {

    @Nested
    class Constructor {


        @Test
        void invalidDropUpper() {
            assertThatThrownBy(() -> new SmoothedAverageView(-1.0, 0.0, Collections.emptySet(), 1000, "name", "unit", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidDropLower() {
            assertThatThrownBy(() -> new SmoothedAverageView(0.05, 1.01, Collections.emptySet(), 1000, "name", "unit", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    class GetSeriesNames {

        @Test
        void checkSmoothedAverageSeries() {
            TimeWindowView view = new SmoothedAverageView(0.0, 0.05, Collections.emptySet(), 10, "name", "unit", "description", 1);

            assertThat(view.getSeriesNames()).containsExactly("name");
        }

    }

    @Nested
    class ComputeMetrics {

        private TagContext createTagContext(String... keyValuePairs) {
            TagContextBuilder builder = Tags.getTagger().emptyBuilder();
            for (int i = 0; i < keyValuePairs.length; i += 2) {
                builder.putLocal(TagKey.create(keyValuePairs[i]), TagValue.create(keyValuePairs[i + 1]));
            }
            return builder.build();
        }

        @Test
        void checkSmoothedAverageMetricDescriptor() {
            TimeWindowView view = new SmoothedAverageView(0.05, 0.05, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 1);

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> result = view.computeMetrics(queryTime);
            assertThat(result).hasSize(1);
            MetricDescriptor descriptor = result.iterator().next().getMetricDescriptor();

            assertThat(descriptor.getName()).isEqualTo("name");
            assertThat(descriptor.getDescription()).isEqualTo("description");
            assertThat(descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(descriptor.getUnit()).isEqualTo("unit");
            assertThat(descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);
        }

        @Test
        void checkSmoothedAverageMetric() {
            TimeWindowView view = new SmoothedAverageView(0.05, 0.05, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 12);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));

            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            view.insertValue(150, Timestamp.fromMillis(4), createTagContext("my_tag", "bar"));
            view.insertValue(171, Timestamp.fromMillis(4), createTagContext("my_tag", "bar"));
            view.insertValue(250, Timestamp.fromMillis(5), createTagContext("my_tag", "bar"));
            view.insertValue(99, Timestamp.fromMillis(5), createTagContext("my_tag", "bar"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "bar"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(2).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(71.75));
                });
            }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(130.75));
                });
            });
        }

        @Test
        void checkSmoothedAverageMetricGreatIndex() {
            TimeWindowView view = new SmoothedAverageView(0.2, 0.2, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 24);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(42, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(8), createTagContext("my_tag", "foo"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(1).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(97.14285714285714));
                });
            });
        }

        @Test
        void checkSmoothedAverageMetricDropOnlyLower() {
            TimeWindowView view = new SmoothedAverageView(0.0, 0.2, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 24);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(42, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(8), createTagContext("my_tag", "foo"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(1).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(123.78947368421052));
                });
            });
        }

        @Test
        void checkSmoothedAverageMetricDropOnlyUpper() {
            TimeWindowView view = new SmoothedAverageView(0.11, 0.0, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 24);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(42, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(8), createTagContext("my_tag", "foo"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(1).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(92.04761904761905));
                });
            });
        }

        @Test
        void checkSmoothedAverageMetricDropGreaterUpperIndex() {
            TimeWindowView view = new SmoothedAverageView(0.9, 0.2, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 24);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(42, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(50, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(68, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(70, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(4), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(5), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(6), createTagContext("my_tag", "foo"));
            view.insertValue(171, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(250, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(7), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(8), createTagContext("my_tag", "foo"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(1).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(68.0));
                });
            });
        }

        @Test
        void checkSmoothedAverageMetricDropNothing() {
            TimeWindowView view = new SmoothedAverageView(0.0, 0.0, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 4);

            view.insertValue(80, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(87, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(100, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));
            view.insertValue(150, Timestamp.fromMillis(3), createTagContext("my_tag", "foo"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);

            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(1).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(104.25));
                });
            });
        }

        @Test
        void checkSmoothedAverageMetricDropSmallValues() {
            TimeWindowView view = new SmoothedAverageView(0.1, 0.0, ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 4);

            view.insertValue(116, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(125, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);

            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(1).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(116));
                });
            });
        }

    }
}

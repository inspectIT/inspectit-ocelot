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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PercentileViewTest {

    @Nested
    class Constructor {

        @Test
        void noPercentilesAndMinMaxSpecified() {
            assertThatThrownBy(() -> new PercentileView(false, false, Collections.emptySet(), Collections.emptySet(), 1000, "name", "unit", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidPercentile() {
            assertThatThrownBy(() -> new PercentileView(true, true, new HashSet<>(Arrays.asList(1.0)), Collections.emptySet(), 1000, "name", "unit", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankName() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(), Collections.emptySet(), 1000, " ", "unit", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankUnit() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(), Collections.emptySet(), 1000, "name", " ", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankDescription() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(), Collections.emptySet(), 1000, "name", "unit", " ", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidTimeWindow() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(), Collections.emptySet(), 0, "name", "unit", "description", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidBufferSize() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(), Collections.emptySet(), 1000, "name", "unit", "description", 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    class GetPercentileTag {

        @Test
        void unnecessaryZeroesOmitted() {
            String tag = PercentileView.getPercentileTag(0.5);
            assertThat(tag).isEqualTo("0.5");
        }

        @Test
        void tooLongValueRoundedDown() {
            String tag = PercentileView.getPercentileTag(1.0 / 3);
            assertThat(tag).isEqualTo("0.33333");
        }

        @Test
        void tooLongValueRoundedUp() {
            String tag = PercentileView.getPercentileTag(1.0 / 3 * 2);
            assertThat(tag).isEqualTo("0.66667");
        }
    }

    @Nested
    class GetSeriesNames {

        @Test
        void checkPercentileSeries() {
            TimeWindowView view = new PercentileView(false, false, ImmutableSet.of(0.5), Collections.emptySet(), 10, "name", "unit", "description", 1);

            assertThat(view.getSeriesNames()).containsExactly("name");
        }

        @Test
        void checkMinSeries() {
            TimeWindowView view = new PercentileView(true, false, Collections.emptySet(), Collections.emptySet(), 10, "name", "unit", "description", 1);

            assertThat(view.getSeriesNames()).containsExactly("name_min");
        }

        @Test
        void checkMaxSeries() {
            TimeWindowView view = new PercentileView(false, true, Collections.emptySet(), Collections.emptySet(), 10, "name", "unit", "description", 1);

            assertThat(view.getSeriesNames()).containsExactly("name_max");
        }

        @Test
        void checkAllPercentileSeries() {
            TimeWindowView view = new PercentileView(true, true, ImmutableSet.of(0.5), Collections.emptySet(), 10, "name", "unit", "description", 1);

            assertThat(view.getSeriesNames()).containsExactlyInAnyOrder("name", "name_min", "name_max");
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
        void checkQuantileMetricDescriptor() {
            TimeWindowView view = new PercentileView(false, false, ImmutableSet.of(0.5), ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 1);

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> result = view.computeMetrics(queryTime);
            assertThat(result).hasSize(1);
            MetricDescriptor descriptor = result.iterator().next().getMetricDescriptor();

            assertThat(descriptor.getName()).isEqualTo("name");
            assertThat(descriptor.getDescription()).isEqualTo("description");
            assertThat(descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""), LabelKey.create("quantile", ""));
            assertThat(descriptor.getUnit()).isEqualTo("unit");
            assertThat(descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);
        }

        @Test
        void checkMinMetricDescriptor() {
            TimeWindowView view = new PercentileView(true, false, Collections.emptySet(), ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 1);

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> result = view.computeMetrics(queryTime);
            assertThat(result).hasSize(1);
            MetricDescriptor descriptor = result.iterator().next().getMetricDescriptor();

            assertThat(descriptor.getName()).isEqualTo("name_min");
            assertThat(descriptor.getDescription()).isEqualTo("description");
            assertThat(descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(descriptor.getUnit()).isEqualTo("unit");
            assertThat(descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);
        }

        @Test
        void checkMaxMetricDescriptor() {
            TimeWindowView view = new PercentileView(false, true, Collections.emptySet(), ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 1);

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> result = view.computeMetrics(queryTime);
            assertThat(result).hasSize(1);
            MetricDescriptor descriptor = result.iterator().next().getMetricDescriptor();

            assertThat(descriptor.getName()).isEqualTo("name_max");
            assertThat(descriptor.getDescription()).isEqualTo("description");
            assertThat(descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(descriptor.getUnit()).isEqualTo("unit");
            assertThat(descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);
        }

        @Test
        void checkMinimumMetric() {
            TimeWindowView view = new PercentileView(true, false, Collections.emptySet(), ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 4);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            view.insertValue(100, Timestamp.fromMillis(4), createTagContext("my_tag", "bar"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(2).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(42));
                });
            }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(100));
                });
            });
        }

        @Test
        void checkMaximumMetric() {
            TimeWindowView view = new PercentileView(false, true, Collections.emptySet(), ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 4);

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            view.insertValue(100, Timestamp.fromMillis(4), createTagContext("my_tag", "bar"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(2).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(99));
                });
            }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(101));
                });
            });
        }

        @Test
        void checkPercentileMetrics() {
            TimeWindowView view = new PercentileView(false, false, ImmutableSet.of(0.5, 0.9), ImmutableSet.of("my_tag"), 10, "name", "unit", "description", 18);

            for (int i = 1; i < 10; i++) {
                view.insertValue(10 + i, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
                view.insertValue(100 + i, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            }

            Timestamp queryTime = Timestamp.fromMillis(10);
            Collection<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);
            Metric result = results.iterator().next();

            assertThat(result.getTimeSeriesList()).hasSize(4).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"), LabelValue.create("0.9"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(19));
                });
            }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"), LabelValue.create("0.9"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(109));
                });
            }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"), LabelValue.create("0.5"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(15));
                });
            }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"), LabelValue.create("0.5"));
                assertThat(series.getPoints()).hasSize(1).anySatisfy(pt -> {
                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(105));
                });

            });

        }

    }
}

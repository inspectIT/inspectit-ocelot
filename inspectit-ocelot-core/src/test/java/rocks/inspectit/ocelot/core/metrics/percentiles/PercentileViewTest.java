package rocks.inspectit.ocelot.core.metrics.percentiles;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PercentileViewTest {

    @Nested
    class Constructor {

        @Test
        void noPercentilesAndMinMaxSpecified() {
            assertThatThrownBy(() -> new PercentileView(false, false, Collections.emptySet(),
                    Collections.emptySet(), 1000, "name", "unit", "description"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidPercentile() {
            assertThatThrownBy(() -> new PercentileView(true, true, new HashSet<>(Arrays.asList(100)),
                    Collections.emptySet(), 1000, "name", "unit", "description"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankName() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(),
                    Collections.emptySet(), 1000, " ", "unit", "description"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankUnit() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(),
                    Collections.emptySet(), 1000, "name", " ", "description"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankDescription() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(),
                    Collections.emptySet(), 1000, "name", "unit", " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidTimeWindow() {
            assertThatThrownBy(() -> new PercentileView(true, true, Collections.emptySet(),
                    Collections.emptySet(), 0, "name", "unit", "description"))
                    .isInstanceOf(IllegalArgumentException.class);
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
        void checkMinimumMetric() {
            PercentileView view = new PercentileView(true, false, Collections.emptySet(),
                    ImmutableSet.of("my_tag"), 10, "name", "unit", "description");

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            view.insertValue(100, Timestamp.fromMillis(4), createTagContext("my_tag", "bar"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            List<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);

            Metric minMetric = results.get(0);
            MetricDescriptor descriptor = minMetric.getMetricDescriptor();
            assertThat(descriptor.getName()).isEqualTo("name_min");
            assertThat(descriptor.getDescription()).isEqualTo("description");
            assertThat(descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(descriptor.getUnit()).isEqualTo("unit");
            assertThat(descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);

            assertThat(minMetric.getTimeSeriesList())
                    .hasSize(2)
                    .anySatisfy(series -> {
                        assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                        assertThat(series.getPoints())
                                .hasSize(1)
                                .anySatisfy(pt -> {
                                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(42));
                                });
                    }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints())
                        .hasSize(1)
                        .anySatisfy(pt -> {
                            assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                            assertThat(pt.getValue()).isEqualTo(Value.doubleValue(100));
                        });
            });
        }

        @Test
        void checkMaximumMetric() {
            PercentileView view = new PercentileView(false, true, Collections.emptySet(),
                    ImmutableSet.of("my_tag"), 10, "name", "unit", "description");

            view.insertValue(42, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
            view.insertValue(99, Timestamp.fromMillis(2), createTagContext("my_tag", "foo"));
            view.insertValue(101, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            view.insertValue(100, Timestamp.fromMillis(4), createTagContext("my_tag", "bar"));

            Timestamp queryTime = Timestamp.fromMillis(10);
            List<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(1);

            Metric minMetric = results.get(0);
            MetricDescriptor descriptor = minMetric.getMetricDescriptor();
            assertThat(descriptor.getName()).isEqualTo("name_max");
            assertThat(descriptor.getDescription()).isEqualTo("description");
            assertThat(descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(descriptor.getUnit()).isEqualTo("unit");
            assertThat(descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);

            assertThat(minMetric.getTimeSeriesList())
                    .hasSize(2)
                    .anySatisfy(series -> {
                        assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                        assertThat(series.getPoints())
                                .hasSize(1)
                                .anySatisfy(pt -> {
                                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(99));
                                });
                    }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints())
                        .hasSize(1)
                        .anySatisfy(pt -> {
                            assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                            assertThat(pt.getValue()).isEqualTo(Value.doubleValue(101));
                        });
            });
        }

        @Test
        void checkPercentileMetrics() {
            PercentileView view = new PercentileView(false, false, ImmutableSet.of(50, 90),
                    ImmutableSet.of("my_tag"), 10, "name", "unit", "description");

            for (int i = 1; i < 10; i++) {
                view.insertValue(10 + i, Timestamp.fromMillis(1), createTagContext("my_tag", "foo"));
                view.insertValue(100 + i, Timestamp.fromMillis(3), createTagContext("my_tag", "bar"));
            }

            Timestamp queryTime = Timestamp.fromMillis(10);
            List<Metric> results = view.computeMetrics(queryTime);
            assertThat(results).hasSize(2);

            Metric p90Metric = results.stream()
                    .filter(m -> m.getMetricDescriptor().getName().equals("name_p90"))
                    .findFirst()
                    .get();
            MetricDescriptor p90descriptor = p90Metric.getMetricDescriptor();
            assertThat(p90descriptor.getDescription()).isEqualTo("description");
            assertThat(p90descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(p90descriptor.getUnit()).isEqualTo("unit");
            assertThat(p90descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);
            assertThat(p90Metric.getTimeSeriesList())
                    .hasSize(2)
                    .anySatisfy(series -> {
                        assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                        assertThat(series.getPoints())
                                .hasSize(1)
                                .anySatisfy(pt -> {
                                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(19));
                                });
                    }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints())
                        .hasSize(1)
                        .anySatisfy(pt -> {
                            assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                            assertThat(pt.getValue()).isEqualTo(Value.doubleValue(109));
                        });
            });

            Metric p50Metric = results.stream()
                    .filter(m -> m.getMetricDescriptor().getName().equals("name_p50"))
                    .findFirst()
                    .get();
            MetricDescriptor p50descriptor = p50Metric.getMetricDescriptor();
            assertThat(p50descriptor.getDescription()).isEqualTo("description");
            assertThat(p50descriptor.getLabelKeys()).containsExactly(LabelKey.create("my_tag", ""));
            assertThat(p50descriptor.getUnit()).isEqualTo("unit");
            assertThat(p50descriptor.getType()).isEqualTo(MetricDescriptor.Type.GAUGE_DOUBLE);
            assertThat(p50Metric.getTimeSeriesList())
                    .hasSize(2)
                    .anySatisfy(series -> {
                        assertThat(series.getLabelValues()).containsExactly(LabelValue.create("foo"));
                        assertThat(series.getPoints())
                                .hasSize(1)
                                .anySatisfy(pt -> {
                                    assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                                    assertThat(pt.getValue()).isEqualTo(Value.doubleValue(15));
                                });
                    }).anySatisfy(series -> {
                assertThat(series.getLabelValues()).containsExactly(LabelValue.create("bar"));
                assertThat(series.getPoints())
                        .hasSize(1)
                        .anySatisfy(pt -> {
                            assertThat(pt.getTimestamp()).isEqualTo(queryTime);
                            assertThat(pt.getValue()).isEqualTo(Value.doubleValue(105));
                        });
            });
        }

    }

}

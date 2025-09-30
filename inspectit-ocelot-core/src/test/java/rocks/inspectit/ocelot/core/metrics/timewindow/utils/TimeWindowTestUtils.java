package rocks.inspectit.ocelot.core.metrics.timewindow.utils;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeWindowTestUtils {

    /**
     * Checks, if the collections of metrics contains the expected series count
     *
     * @param metrics the collection of metrics
     * @param expectedSeriesCount the expected series count
     */
    public static void assertTotalSeriesCount(Collection<MetricData> metrics, long expectedSeriesCount) {
        long count = metrics
                .stream()
                .mapToLong(metric -> metric.getData().getPoints().size())
                .sum();
        assertThat(count).isEqualTo(expectedSeriesCount);
    }

    /**
     * Checks, if the collection of metrics contains a metric with the expected time series data (= value + attributes).
     *
     * @param metrics the collection of metrics
     * @param name the metric name to check
     * @param expectedValue the expected value for the time series
     * @param expectedAttributes the expected attributes for the time series
     */
    public static void assertContainsData(Collection<MetricData> metrics, String name, double expectedValue, Map<String,String> expectedAttributes) {
        assertThat(metrics)
                .anySatisfy(m -> assertThat(m.getName()).isEqualTo(name));
        MetricData metric = metrics.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .get();

        assertThat(metric.getDoubleGaugeData().getPoints())
                .anyMatch(point -> containsExpectedTimeSeries(point, expectedValue, expectedAttributes));
    }

    /**
     * Checks, if the expected values exists for the requested time series (== attributes)
     *
     * @param pointData the data point of the metric
     * @param expectedValue the expected value
     * @param expectedAttributes the expected attributes
     *
     * @return true, if the data point contains all expected attributes and value
     */
    private static boolean containsExpectedTimeSeries(DoublePointData pointData, double expectedValue, Map<String,String> expectedAttributes) {
        if (!(expectedValue == pointData.getValue())) return false;

        if (CollectionUtils.isEmpty(expectedAttributes)) {
            return pointData.getAttributes().isEmpty();
        } else {
            for (Map.Entry<String, String> keyValuePair : expectedAttributes.entrySet()) {
                String key = keyValuePair.getKey();
                String expectedAttributeValue = keyValuePair.getValue();
                String value = pointData.getAttributes()
                        .get(AttributeKey.stringKey(key));

                boolean containsAttribute = expectedAttributeValue.equals(value);
                if(!containsAttribute) return false;
            }
            return true;
        }
    }
}

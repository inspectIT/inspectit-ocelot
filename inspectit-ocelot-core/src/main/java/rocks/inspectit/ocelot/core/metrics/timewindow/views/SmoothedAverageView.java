package rocks.inspectit.ocelot.core.metrics.timewindow.views;

import io.opentelemetry.api.common.Attributes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * For the data within this window, smoothed averages can be computed.
 */
@Slf4j
public class SmoothedAverageView extends TimeWindowView {

    /**
     * The metric information for this view, if smoothed average
     */
    private final MetricInfo metricInfo;

    @Getter
    private final double dropUpper;

    @Getter
    private final double dropLower;

    /**
     * @param viewName         the prefix to use for the names of all exposed metrics
     * @param description      the description of this view
     * @param unit             the unit of the metric
     * @param attributes       the attribute keys to use for this view
     * @param timeWindow       the time rang to use for computing smoothed average values
     * @param bufferLimit      the maximum number of data-points to be buffered by this view
     * @param dropUpper        value in percentage in the range (0,1) which indicates how many metrics in the upper range shall be dropped
     * @param dropLower        value in percentage in the range (0,1) which indicates how many metrics in the lower range shall be dropped
     */
    public SmoothedAverageView(String viewName, String description, String unit, Set<String> attributes,
                               Duration timeWindow, int bufferLimit, double dropUpper, double dropLower) {
        super(viewName, description, unit, attributes, timeWindow, bufferLimit);
        validateConfiguration(dropUpper, dropLower);

        this.dropUpper = dropUpper;
        this.dropLower = dropLower;

        this.metricInfo = new MetricInfo(viewName, description, unit);
    }

    private void validateConfiguration(double dropUpper, double dropLower) {
        if (dropUpper < 0.0 || dropUpper > 1.0) {
            throw new IllegalArgumentException("dropUpper must be greater than 0.0 and smaller than 1.0!");
        }
        if (dropLower < 0.0 || dropLower > 1.0) {
            throw new IllegalArgumentException("dropLower must be greater than 0.0 and smaller than 1.0!");
        }
    }

    /**
     * @return true, if this view uses the same properties as provided
     */
    public boolean isSameConfiguration(String description, String unit, Set<String> attributes, Duration timeWindow,
                                       int bufferLimit, double dropUpper, double dropLower) {
        return this.description.equals(description) &&
                this.unit.equals(unit) &&
                this.timeWindow.equals(timeWindow) &&
                this.bufferLimit == bufferLimit &&
                this.getAttributeKeys().equals(attributes) &&
                this.dropUpper == dropUpper &&
                this.dropLower == dropLower;
    }

    @Override
    protected List<MetricInfo> getMetrics() {
        return Collections.singletonList(metricInfo);
    }

    @Override
    protected void computeSeries(ResultSeriesCollector resultSeries, Instant time, Attributes attributes, double[] data) {
        int queueLength = data.length;

        int skipAtBottom = Math.min((int) Math.ceil(dropLower * queueLength), queueLength - 1);
        int skipAtTop = Math.min((int) Math.ceil(dropUpper * queueLength), queueLength - 1);
        int limit = Math.max(queueLength - skipAtBottom - skipAtTop, 1);

        double smoothedAverage = Arrays.stream(data)
                .sorted()
                .skip(skipAtBottom)
                .limit(limit)
                .average()
                .orElse(0.0);
        resultSeries.add(metricInfo, time, attributes, smoothedAverage);
    }
}

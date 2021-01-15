package rocks.inspectit.oce.eum.server.metrics.percentiles;

import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.export.MetricDescriptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static java.lang.Math.round;

/**
 * For the data within this window, smoothed averages can be computed.
 */
@Slf4j
public class SmoothedAverageView extends TimeWindowView {

    /**
     * The descriptor of the metric for this view, if smoothed average.
     */
    private MetricDescriptor metricDescriptor;

    @Getter
    private double dropUpper;

    @Getter
    private double dropLower;

    /**
     * Constructor.
     *
     * @param dropUpper        value in percentage in the range (0,1) which indicates how many metrics in the upper range shall be dropped
     * @param dropLower        value in percentage in the range (0,1) which indicates how many metrics in the lower range shall be dropped
     * @param tags             the tags to use for this view
     * @param timeWindowMillis the time range in milliseconds to use for computing minimum / maximum and percentile values
     * @param viewName         the prefix to use for the names of all exposed metrics
     * @param unit             the unit of the measure
     * @param description      the description of this view
     * @param bufferLimit      the maximum number of measurements to be buffered by this view
     */
    SmoothedAverageView(double dropUpper, double dropLower, Set<String> tags, long timeWindowMillis, String viewName, String unit, String description, int bufferLimit) {
        super(tags, timeWindowMillis, viewName, unit, description, bufferLimit);
        validateConfiguration(dropUpper, dropLower);

        this.dropUpper = dropUpper;
        this.dropLower = dropLower;

        List<LabelKey> smoothedAverageLabelKeys = getLabelKeysInOrder();
        metricDescriptor = MetricDescriptor.create(viewName, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, smoothedAverageLabelKeys);
    }

    private void validateConfiguration(double dropUpper, double dropLower) {
        if (dropUpper < 0.0 || dropUpper > 1.0) {
            throw new IllegalArgumentException("dropUpper must be greater than 0.0 and smaller than 1.0!");
        }
        if (dropLower < 0.0 || dropLower > 1.0) {
            throw new IllegalArgumentException("dropLower must be greater than 0.0 and smaller than 1.0!");
        }
    }

    @Override
    Set<String> getSeriesNames() {
        Set<String> result = new HashSet<>();
        result.add(metricDescriptor.getName());
        return result;
    }

    @Override
    protected List<MetricDescriptor> getMetrics() {
        return Collections.singletonList(metricDescriptor);
    }

    @Override
    protected void computeSeries(List<String> tagValues, double[] data, Timestamp time, ResultSeriesCollector resultSeries) {
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
        resultSeries.add(metricDescriptor, smoothedAverage, time, tagValues);
    }

}

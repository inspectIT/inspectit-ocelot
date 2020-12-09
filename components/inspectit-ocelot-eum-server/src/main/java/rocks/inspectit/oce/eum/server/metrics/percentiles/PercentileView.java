package rocks.inspectit.oce.eum.server.metrics.percentiles;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.export.MetricDescriptor;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * For the data within this window, percentiles and min / max values can be computed.
 */
public class PercentileView extends TimeWindowView {

    /**
     * The tag to use for the percentile or "min","max" respectively.
     */
    private static final String PERCENTILE_TAG_KEY = "quantile";

    /**
     * The tag value to use for {@link #PERCENTILE_TAG_KEY} for the "minimum" series.
     */
    private static final String MIN_METRIC_SUFFIX = "_min";

    /**
     * The tag value to use for {@link #PERCENTILE_TAG_KEY} for the "maximum" series.
     */
    private static final String MAX_METRIC_SUFFIX = "_max";

    /**
     * The formatter used to print percentiles to tags.
     */
    private static final DecimalFormat PERCENTILE_TAG_FORMATTER = new DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    /**
     * The descriptor of the metric for this view, if percentile.
     */
    private MetricDescriptor percentileMetricDescriptor;

    /**
     * If not null, the minimum value will be exposed as this gauge.
     */
    private MetricDescriptor minMetricDescriptor;

    /**
     * If not null, the maximum value will be exposed as this gauge.
     */
    private MetricDescriptor maxMetricDescriptor;

    /**
     * The percentiles to compute in the range (0,1)
     */
    @Getter
    private Set<Double> percentiles;

    /**
     * Constructor.
     *
     * @param includeMin       true, if the minimum value should be exposed as metric
     * @param includeMax       true, if the maximum value should be exposed as metric
     * @param percentiles      the set of percentiles in the range (0,1) which shall be provided as metrics
     * @param tags             the tags to use for this view
     * @param timeWindowMillis the time range in milliseconds to use for computing minimum / maximum and percentile values
     * @param viewName         the prefix to use for the names of all exposed metrics
     * @param unit             the unit of the measure
     * @param description      the description of this view
     * @param bufferLimit      the maximum number of measurements to be buffered by this view
     */
    PercentileView(boolean includeMin, boolean includeMax, Set<Double> percentiles, Set<String> tags, long timeWindowMillis, String viewName, String unit, String description, int bufferLimit) {
        super(tags, timeWindowMillis, viewName, unit, description, bufferLimit);
        validateConfiguration(includeMin, includeMax, percentiles);

        this.percentiles = new HashSet<>(percentiles);

        List<LabelKey> percentileLabelKeys = getLabelKeysInOrder(PERCENTILE_TAG_KEY);
        List<LabelKey> minMaxLabelKeys = getLabelKeysInOrder();
        if (!percentiles.isEmpty()) {
            percentileMetricDescriptor = MetricDescriptor.create(viewName, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, percentileLabelKeys);
        }
        if (includeMin) {
            minMetricDescriptor = MetricDescriptor.create(viewName + MIN_METRIC_SUFFIX, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, minMaxLabelKeys);
        }
        if (includeMax) {
            maxMetricDescriptor = MetricDescriptor.create(viewName + MAX_METRIC_SUFFIX, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, minMaxLabelKeys);
        }
    }

    private void validateConfiguration(boolean includeMin, boolean includeMax, Set<Double> percentiles) {
        percentiles.stream().filter(p -> p <= 0.0 || p >= 1.0).forEach(p -> {
            throw new IllegalArgumentException("Percentiles must be in range (0,1)");
        });
        if (percentiles.isEmpty() && !includeMin && !includeMax) {
            throw new IllegalArgumentException("You must specify at least one percentile or enable minimum or maximum computation!");
        }
    }

    @Override
    Set<String> getSeriesNames() {
        Set<String> result = new HashSet<>();
        if (minMetricDescriptor != null) {
            result.add(minMetricDescriptor.getName());
        }
        if (maxMetricDescriptor != null) {
            result.add(maxMetricDescriptor.getName());
        }
        if (!percentiles.isEmpty()) {
            result.add(percentileMetricDescriptor.getName());
        }
        return result;
    }

    boolean isMinEnabled() {
        return minMetricDescriptor != null;
    }

    boolean isMaxEnabled() {
        return maxMetricDescriptor != null;
    }

    @VisibleForTesting
    static String getPercentileTag(double percentile) {
        return PERCENTILE_TAG_FORMATTER.format(percentile);
    }

    @Override
    protected List<MetricDescriptor> getMetrics() {
        List<MetricDescriptor> metrics = new ArrayList<>();
        if (isMinEnabled()) {
            metrics.add(minMetricDescriptor);
        }
        if (isMaxEnabled()) {
            metrics.add(maxMetricDescriptor);
        }
        if (!percentiles.isEmpty()) {
            metrics.add(percentileMetricDescriptor);
        }
        return metrics;
    }

    @Override
    protected void computeSeries(List<String> tagValues, double[] data, Timestamp time, ResultSeriesCollector resultSeries) {
        if (isMinEnabled() || isMaxEnabled()) {
            double minValue = Double.MAX_VALUE;
            double maxValue = -Double.MAX_VALUE;
            for (double value : data) {
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
            if (isMinEnabled()) {
                resultSeries.add(minMetricDescriptor, minValue, time, tagValues);
            }
            if (isMaxEnabled()) {
                resultSeries.add(maxMetricDescriptor, maxValue, time, tagValues);
            }
        }
        if (!percentiles.isEmpty()) {
            Percentile percentileComputer = new Percentile();
            percentileComputer.setData(data);
            for (double percentile : percentiles) {
                double percentileValue = percentileComputer.evaluate(percentile * 100);
                List<String> percentileTagValues = new ArrayList<>(tagValues);
                percentileTagValues.add(getPercentileTag(percentile));
                resultSeries.add(percentileMetricDescriptor, percentileValue, time, percentileTagValues);
            }
        }
    }

}

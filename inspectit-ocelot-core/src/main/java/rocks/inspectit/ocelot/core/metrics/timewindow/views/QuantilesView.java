package rocks.inspectit.ocelot.core.metrics.timewindow.views;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.Attributes;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * For the data within this window, percentiles and min / max values can be computed.
 */
public class QuantilesView extends TimeWindowView {

    /**
     * The attribute to use for the quantile or "min","max" respectively.
     */
    private static final String QUANTILE_ATTRIBUTE_KEY = "quantile";

    /**
     * The attribute value to use for {@link #QUANTILE_ATTRIBUTE_KEY} for the "minimum" series.
     */
    private static final String MIN_METRIC_SUFFIX = "_min";

    /**
     * The attribute value to use for {@link #QUANTILE_ATTRIBUTE_KEY} for the "maximum" series.
     */
    private static final String MAX_METRIC_SUFFIX = "_max";

    /**
     * The formatter used to print quantiles to attributes.
     */
    private static final DecimalFormat QUANTILE_TAG_FORMATTER = new DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    /**
     * The descriptor of the metric for this view, if quantile.
     */
    private MetricInfo quantileMetricInfo;

    /**
     * If not null, the minimum value will be exposed as this gauge.
     */
    private MetricInfo minMetricInfo;

    /**
     * If not null, the maximum value will be exposed as this gauge.
     */
    private MetricInfo maxMetricInfo;

    /**
     * The quantiles to compute in the range (0,1)
     */
    @Getter
    private final Set<Double> quantiles;

    /**
     * @param viewName    the prefix to use for the names of all exposed metrics
     * @param description the description of this view
     * @param unit        the unit of the measure
     * @param attributes  the attribute keys to use for this view
     * @param timeWindow  the time range to use for computing minimum / maximum and quantile values
     * @param bufferLimit the maximum number of measurements to be buffered by this view
     * @param quantiles   the set of quantiles in the range (0,1) which shall be provided as metrics
     * @param includeMax  true, if the maximum value should be exposed as metric
     * @param includeMin  true, if the minimum value should be exposed as metric
     */
    public QuantilesView(String viewName, String description, String unit, Set<String> attributes, Duration timeWindow, int bufferLimit,
                         Set<Double> quantiles, boolean includeMax, boolean includeMin) {
        super(viewName, description, unit, attributes, timeWindow, bufferLimit);
        validateConfiguration(includeMin, includeMax, quantiles);

        this.quantiles = new HashSet<>(quantiles);

        if (!quantiles.isEmpty()) {
            this.quantileMetricInfo = new MetricInfo(viewName, description, unit);
        }
        if (includeMin) {
            this.minMetricInfo = new MetricInfo(viewName + MIN_METRIC_SUFFIX, description, unit);
        }
        if (includeMax) {
            this.maxMetricInfo = new MetricInfo(viewName + MAX_METRIC_SUFFIX, description, unit);
        }
    }

    private void validateConfiguration(boolean includeMin, boolean includeMax, Set<Double> quantiles) {
        quantiles.stream().filter(q -> q <= 0.0 || q >= 1.0).forEach(q -> {
            throw new IllegalArgumentException("Quantiles must be in range (0,1)");
        });
        if (quantiles.isEmpty() && !includeMin && !includeMax) {
            throw new IllegalArgumentException("You must specify at least one quantile or enable minimum or maximum computation!");
        }
    }

    /**
     * @return true, if this view uses the same properties as provided
     */
    public boolean isSameConfiguration(String description, String unit, Set<String> attributes, Duration timeWindow,
                                       int bufferLimit, Collection<Double> quantiles, boolean includeMin, boolean includeMax) {
        return this.description.equals(description) &&
                this.unit.equals(unit) &&
                this.timeWindow.equals(timeWindow) &&
                this.bufferLimit == bufferLimit &&
                this.getAttributeKeys().equals(attributes) &&
                this.quantiles.equals(quantiles) &&
                this.isMinEnabled() == includeMin &&
                this.isMaxEnabled() == includeMax;
    }

    public boolean isMinEnabled() {
        return minMetricInfo != null;
    }

    public boolean isMaxEnabled() {
        return maxMetricInfo != null;
    }

    @VisibleForTesting
    static String getQuantileAttribute(double quantile) {
        return QUANTILE_TAG_FORMATTER.format(quantile);
    }

    @Override
    protected List<MetricInfo> getMetrics() {
        List<MetricInfo> metrics = new ArrayList<>();
        if (isMinEnabled()) {
            metrics.add(minMetricInfo);
        }
        if (isMaxEnabled()) {
            metrics.add(maxMetricInfo);
        }
        if (!quantiles.isEmpty()) {
            metrics.add(quantileMetricInfo);
        }
        return metrics;
    }

    @Override
    protected void computeSeries(ResultSeriesCollector resultSeries, Instant time, Attributes attributes, double[] data) {
        if (isMinEnabled() || isMaxEnabled()) {
            double minValue = Double.MAX_VALUE;
            double maxValue = -Double.MAX_VALUE;
            for (double value : data) {
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
            if (isMinEnabled()) {
                resultSeries.add(minMetricInfo, time, attributes, minValue);
            }
            if (isMaxEnabled()) {
                resultSeries.add(maxMetricInfo, time, attributes, maxValue);
            }
        }
        if (!quantiles.isEmpty()) {
            Percentile percentileComputer = new Percentile();
            percentileComputer.setData(data);
            for (double quantile : quantiles) {
                double percentileValue = percentileComputer.evaluate(quantile * 100);
                Attributes attributesWithQuantile = attributes.toBuilder()
                        .put(QUANTILE_ATTRIBUTE_KEY, getQuantileAttribute(quantile))
                        .build();
                resultSeries.add(quantileMetricInfo, time, attributesWithQuantile, percentileValue);
            }
        }
    }
}

package rocks.inspectit.ocelot.core.metrics.percentiles;

import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.export.*;
import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.Tag;
import io.opencensus.tags.TagContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Holds the data for a given measurement splitted by a provided set of tags over a given time window.
 * For the data within this window, percentiles and min / max values can be computed.
 */
public class PercentileView {

    /**
     * The name suffix to use for the exposed "min" metric.
     * E.g. if the view name is "my/cool/view", the resulting metric will be "my/cool/view_min"
     */
    private static final String MIN_METRIC_NAME_SUFFIX = "_min";

    /**
     * The name suffix to use for the exposed "max" metric.
     * E.g. if the view name is "my/cool/view", the resulting metric will be "my/cool/view_max"
     */
    private static final String MAX_METRIC_NAME_SUFFIX = "_max";

    /**
     * The name suffix to use for each percentile metric.
     * E.g. if the view name is "my/cool/view", the resulting metric for example for the 95 percentile will be "my/cool/view_p95"
     */
    private static final String PERCENTILE_METRIC_NAME_SUFFIX = "_p";

    /**
     * Metric descriptor for the metric providing the minimum observed value in the given time frame.
     * If this is null, minimum computation is disabled;
     */
    private MetricDescriptor minMetricDescriptor;

    /**
     * Metric descriptor for the metric providing the maximum observed value in the given time frame.
     * If this is null, maximum computation is disabled;
     */
    private MetricDescriptor maxMetricDescriptor;

    /**
     * Maps percentiles in the range 1 to 99 to their metric descriptor.
     * The key set of this map defines for which percentiles metrics are exposed.
     */
    private Map<Integer, MetricDescriptor> percentileMetrics;

    /**
     * Defines the tags which are used for the view.
     * E.g. if the tag "http_path" is used, percentiles will be computed for each http_path individually.
     * <p>
     * The tag values are stored in a fixed order in the keys of {@link #seriesValues} for each series.
     * The values within {@link #tagIndices} define at which position within these arrays the corresponding tag value is found.
     * E.g. if tagIndex["http_path"] = 2, this means that the values for http_path will be at index 2 in the keys of {@link #seriesValues}.
     */
    private Map<String, Integer> tagIndices;

    /**
     * Stores the buffered data of the sliding time window for each time series.
     */
    private ConcurrentHashMap<List<String>, WindowedDoubleQueue> seriesValues;

    /**
     * Defines the size of the sliding window in milliseconds.
     * E.g. a value of 15000 means that percentiles will be computed based on the last 15 seconds.
     */
    @Getter
    private long timeWindowMillis;

    /**
     * The name of the view, used as prefix for all individual metrics.
     */
    @Getter
    private String baseViewName;

    /**
     * The unit of the measure.
     */
    @Getter
    private String unit;

    /**
     * The description of this view.
     */
    @Getter
    private String description;

    /**
     * Constructor.
     *
     * @param includeMin       true, if the minimum value should be exposed as metric
     * @param includeMax       true, if the maximum value should be exposed as metric
     * @param percentiles      the set of percentiles in the range 1 to 99 which shall be provided as metrics
     * @param tags             the tags to use for this view
     * @param timeWindowMillis the time range in milliseconds to use for computing minimum / maximum and percentile values
     * @param baseViewName     the prefix to use for the names of all exposed metrics
     * @param unit             the unit of the measure
     * @param description      the description of this view
     */
    PercentileView(boolean includeMin, boolean includeMax, Set<Integer> percentiles, Set<String> tags, long timeWindowMillis, String baseViewName, String unit, String description) {
        validateConfiguration(includeMin, includeMax, percentiles, timeWindowMillis, baseViewName, unit, description);
        assignTagIndices(tags);
        seriesValues = new ConcurrentHashMap<>();
        this.timeWindowMillis = timeWindowMillis;
        this.baseViewName = baseViewName;
        this.unit = unit;
        this.description = description;

        List<LabelKey> labelKeys = getLabelKeysInOrder();

        if (includeMin) {
            minMetricDescriptor = MetricDescriptor.create(baseViewName + MIN_METRIC_NAME_SUFFIX, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, labelKeys);
        }
        if (includeMax) {
            maxMetricDescriptor = MetricDescriptor.create(baseViewName + MAX_METRIC_NAME_SUFFIX, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, labelKeys);
        }
        percentileMetrics = new HashMap<>();
        for (int percentile : percentiles) {
            String name = baseViewName + PERCENTILE_METRIC_NAME_SUFFIX + String.format("%02d", percentile);
            MetricDescriptor percentileMetric = MetricDescriptor.create(name, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, labelKeys);
            percentileMetrics.put(percentile, percentileMetric);
        }
    }

    private void validateConfiguration(boolean includeMin, boolean includeMax, Set<Integer> percentiles, long timeWindowMillis,
                                       String baseViewName, String unit, String description) {
        percentiles.stream()
                .filter(p -> p <= 0 || p >= 100)
                .forEach(p -> {
                    throw new IllegalArgumentException("Percentiles must be in range (0,100)");
                });
        if (StringUtils.isBlank(baseViewName)) {
            throw new IllegalArgumentException("View name must not be blank!");
        }
        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("Description must not be blank!");
        }
        if (StringUtils.isBlank(unit)) {
            throw new IllegalArgumentException("Unit must not be blank!");
        }
        if (timeWindowMillis <= 0) {
            throw new IllegalArgumentException("Time window must not be positive!");
        }
        if (percentiles.isEmpty() && !includeMin && !includeMax) {
            throw new IllegalArgumentException("You must specify at least one percentile or enable minimum or maximum computation!");
        }
    }

    private void assignTagIndices(Set<String> tags) {
        tagIndices = new HashMap<>();
        int idx = 0;
        for (String tag : tags) {
            tagIndices.put(tag, idx);
            idx++;
        }
    }

    /**
     * Adds the provided value to the sliding window of data.
     *
     * @param value      the value of the measure
     * @param time       the timestamp when this value was observed
     * @param tagContext the tags with which this value was observed
     */
    void insertValue(double value, Timestamp time, TagContext tagContext) {
        List<String> tags = getTagsList(tagContext);
        WindowedDoubleQueue queue = seriesValues.computeIfAbsent(tags, (key) -> new WindowedDoubleQueue(timeWindowMillis));
        synchronized (queue) {
            queue.insert(value, getInMillis(time));
        }
    }

    /**
     * @return true, if the minimum is configured to be exposed as metric
     */
    boolean isMinEnabled() {
        return minMetricDescriptor != null;
    }

    /**
     * @return true, if the maximum is configured to be exposed as metric
     */
    boolean isMaxEnabled() {
        return maxMetricDescriptor != null;
    }

    /**
     * @return the percentiles for which metrics will be exposed
     */
    Set<Integer> getPercentiles() {
        return percentileMetrics.keySet();
    }

    /**
     * @return the tags used for this view
     */
    Set<String> getTagKeys() {
        return tagIndices.keySet();
    }

    /**
     * Computes the definined percentile and min / max metrics.
     *
     * @param time the current timestamp
     *
     * @return the list of metrics containing the percentiles and min / max
     */
    List<Metric> computeMetrics(Timestamp time) {
        ResultSeriesCollector collector = computeSeries(time);
        List<Metric> result = new ArrayList<>();
        if (!collector.minSeries.isEmpty()) {
            Metric minMetric = Metric.create(minMetricDescriptor, collector.minSeries);
            result.add(minMetric);
        }
        if (!collector.maxSeries.isEmpty()) {
            Metric minMetric = Metric.create(maxMetricDescriptor, collector.maxSeries);
            result.add(minMetric);
        }
        collector.percentileSeries.forEach((percentile, series) -> {
            Metric percentileMetric = Metric.create(percentileMetrics.get(percentile), series);
            result.add(percentileMetric);
        });
        return result;
    }

    private ResultSeriesCollector computeSeries(Timestamp time) {
        ResultSeriesCollector collector = new ResultSeriesCollector();
        for (Map.Entry<List<String>, WindowedDoubleQueue> series : seriesValues.entrySet()) {
            List<String> tagValues = series.getKey();
            WindowedDoubleQueue queue = series.getValue();
            double[] data = null;
            synchronized (queue) {
                queue.removeStaleValues(getInMillis(time));
                int size = queue.size();
                if (size > 0) {
                    data = queue.copy();
                }
            }
            if (data != null) {
                computeSeries(tagValues, data, time, collector);
            }
        }
        return collector;
    }

    private void computeSeries(List<String> tagValues, double[] data, Timestamp time, ResultSeriesCollector results) {
        List<LabelValue> labelValues = toLabelValues(tagValues);
        if (isMinEnabled() || isMaxEnabled()) {
            double minValue = Double.MAX_VALUE;
            double maxValue = -Double.MAX_VALUE;
            for (double value : data) {
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
            if (isMinEnabled()) {
                results.addMinimum(minValue, time, labelValues);
            }
            if (isMaxEnabled()) {
                results.addMaximum(maxValue, time, labelValues);
            }
        }
        Percentile percentileComputer = null;
        for (int percentile : percentileMetrics.keySet()) {
            if (percentileComputer == null) {
                percentileComputer = new Percentile();
                percentileComputer.setData(data);
            }
            double percentileValue = percentileComputer.evaluate(percentile);
            results.addPercentile(percentile, percentileValue, time, labelValues);
        }
    }

    private List<String> getTagsList(TagContext tagContext) {
        String[] tagValues = new String[tagIndices.size()];
        for (int i = 0; i < tagValues.length; i++) {
            tagValues[i] = "";
        }
        for (Iterator<Tag> it = InternalUtils.getTags(tagContext); it.hasNext(); ) {
            Tag tag = it.next();
            String tagKey = tag.getKey().getName();
            Integer index = tagIndices.get(tagKey);
            if (index != null) {
                tagValues[index] = tag.getValue().asString();
            }
        }
        return Arrays.asList(tagValues);
    }

    private List<LabelValue> toLabelValues(List<String> tagValues) {
        return tagValues.stream().map(LabelValue::create).collect(Collectors.toList());
    }

    private List<LabelKey> getLabelKeysInOrder() {
        LabelKey[] keys = new LabelKey[tagIndices.size()];
        tagIndices.forEach((tag, index) -> keys[index] = LabelKey.create(tag, ""));
        return Arrays.asList(keys);
    }

    private long getInMillis(Timestamp time) {
        return time.getSeconds() * 1000 + time.getNanos() / 1000L / 1000L;
    }

    private class ResultSeriesCollector {

        private List<TimeSeries> minSeries = new ArrayList<>();

        private List<TimeSeries> maxSeries = new ArrayList<>();

        private Map<Integer, List<TimeSeries>> percentileSeries = new HashMap<>();

        void addMinimum(double value, Timestamp time, List<LabelValue> labelValues) {
            Point pt = Point.create(Value.doubleValue(value), time);
            minSeries.add(TimeSeries.createWithOnePoint(labelValues, pt, time));
        }

        void addMaximum(double value, Timestamp time, List<LabelValue> labelValues) {
            Point pt = Point.create(Value.doubleValue(value), time);
            maxSeries.add(TimeSeries.createWithOnePoint(labelValues, pt, time));
        }

        void addPercentile(int percentile, double value, Timestamp time, List<LabelValue> labelValues) {
            Point pt = Point.create(Value.doubleValue(value), time);
            TimeSeries series = TimeSeries.createWithOnePoint(labelValues, pt, time);
            percentileSeries.computeIfAbsent(percentile, (key) -> new ArrayList<>()).add(series);
        }
    }
}

package rocks.inspectit.ocelot.core.metrics.percentiles;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.export.*;
import io.opencensus.tags.InternalUtils;
import io.opencensus.tags.Tag;
import io.opencensus.tags.TagContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Holds the data for a given measurement splitted by a provided set of tags over a given time window.
 * For the data within this window, percentiles and min / max values can be computed.
 */
@Slf4j
public class PercentileView {

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
     * The descriptor of the metric for this view.
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
    private String viewName;

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
     * The maximum amount of measurement points to buffer.
     * If this limit is reached, new measuremetns will be rejected until there is space again.
     */
    @Getter
    private int bufferLimit;

    private boolean overflowWarningPrinted = false;

    /**
     * The current number of points stored in this view, limited by {@link #bufferLimit}.
     */
    private AtomicInteger numberOfPoints;

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
        validateConfiguration(includeMin, includeMax, percentiles, timeWindowMillis, viewName, unit, description, bufferLimit);
        assignTagIndices(tags);
        seriesValues = new ConcurrentHashMap<>();
        this.timeWindowMillis = timeWindowMillis;
        this.viewName = viewName;
        this.unit = unit;
        this.description = description;
        this.percentiles = new HashSet<>(percentiles);
        this.bufferLimit = bufferLimit;
        numberOfPoints = new AtomicInteger(0);

        List<LabelKey> percentileLabelKeys = getLabelKeysInOrderForPercentiles();
        List<LabelKey> minMaxLabelKeys = getLabelKeysInOrderForMinMax();
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

    private void validateConfiguration(boolean includeMin, boolean includeMax, Set<Double> percentiles, long timeWindowMillis,
                                       String baseViewName, String unit, String description, int bufferLimit) {
        percentiles.stream()
                .filter(p -> p <= 0.0 || p >= 1.0)
                .forEach(p -> {
                    throw new IllegalArgumentException("Percentiles must be in range (0,1)");
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
        if (bufferLimit < 1) {
            throw new IllegalArgumentException("The buffer limit must be greater than or equal to 1!");
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
     *
     * @return true, if the point could be added, false otherwise.
     */
    boolean insertValue(double value, Timestamp time, TagContext tagContext) {
        List<String> tags = getTagsList(tagContext);
        if (acquireSpaceForPoint(tags, time)) {
            WindowedDoubleQueue queue = seriesValues.computeIfAbsent(tags, (key) -> new WindowedDoubleQueue(timeWindowMillis));
            synchronized (queue) {
                int removed = queue.insert(value, getInMillis(time));
                numberOfPoints.getAndAdd(-removed);
            }
            return true;
        } else {
            if (!overflowWarningPrinted) {
                overflowWarningPrinted = true;
                log.warn("Dropping points for Percentiles-View '{}' because the buffer limit has been reached!" +
                        " Quantiles will be inaccurate." +
                        " This warning will not be shwon for future drops!", viewName);
            }
            return false;
        }
    }

    /**
     * Removes all data which ihas fallen out of the time window based on the given timestamp.
     * <p>
     * Should be called regularly to avoid stale series blocking new points in other series from acquiring a place.
     *
     * @param time the current time
     */
    public void removeStalePoints(Timestamp time) {
        long timeInMillis = getInMillis(time);
        for (WindowedDoubleQueue queue : seriesValues.values()) {
            synchronized (queue) {
                int removed = queue.removeStaleValues(timeInMillis);
                numberOfPoints.getAndAdd(-removed);
            }
        }
    }

    /**
     * @return the tags used for this view
     */
    Set<String> getTagKeys() {
        return tagIndices.keySet();
    }

    /**
     * Computes the defined percentile and min / max metrics.
     *
     * @param time the current timestamp
     *
     * @return the metrics containing the percentiles and min / max
     */
    Collection<Metric> computeMetrics(Timestamp time) {
        removeStalePoints(time);
        ResultSeriesCollector resultSeries = new ResultSeriesCollector();
        for (Map.Entry<List<String>, WindowedDoubleQueue> series : seriesValues.entrySet()) {
            List<String> tagValues = series.getKey();
            WindowedDoubleQueue queue = series.getValue();
            double[] data = null;
            synchronized (queue) {
                int size = queue.size();
                if (size > 0) {
                    data = queue.copy();
                }
            }
            if (data != null) {
                computeSeries(tagValues, data, time, resultSeries);
            }
        }
        List<Metric> resultMetrics = new ArrayList<>();
        if (!percentiles.isEmpty()) {
            resultMetrics.add(Metric.create(percentileMetricDescriptor, resultSeries.percentileSeries));
        }
        if (isMinEnabled()) {
            resultMetrics.add(Metric.create(minMetricDescriptor, resultSeries.minSeries));
        }
        if (isMaxEnabled()) {
            resultMetrics.add(Metric.create(maxMetricDescriptor, resultSeries.maxSeries));
        }
        return resultMetrics;
    }

    private boolean acquireSpaceForPoint(List<String> tags, Timestamp time) {
        int currentSize = numberOfPoints.get();
        if (currentSize == bufferLimit) {
            // try to make some space in our own queue
            // we do not loop over all queues for each insert, as this might be a big number of series
            WindowedDoubleQueue queue = seriesValues.get(tags);
            if (queue != null) {
                synchronized (queue) {
                    int removed = queue.removeStaleValues(getInMillis(time));
                    numberOfPoints.getAndAdd(-removed);
                }
            }
        }
        //try to earn a place until the queue is full again
        while (true) {
            currentSize = numberOfPoints.get();
            if (currentSize == bufferLimit) {
                return false;
            }
            boolean success = numberOfPoints.compareAndSet(currentSize, currentSize + 1);
            if (success) {
                return true;
            }
        }
    }

    boolean isMinEnabled() {
        return minMetricDescriptor != null;
    }

    boolean isMaxEnabled() {
        return maxMetricDescriptor != null;
    }

    private void computeSeries(List<String> tagValues, double[] data, Timestamp time, ResultSeriesCollector resultSeries) {
        if (isMinEnabled() || isMaxEnabled()) {
            double minValue = Double.MAX_VALUE;
            double maxValue = -Double.MAX_VALUE;
            for (double value : data) {
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
            if (isMinEnabled()) {
                resultSeries.addMinimum(minValue, time, tagValues);
            }
            if (isMaxEnabled()) {
                resultSeries.addMaximum(maxValue, time, tagValues);
            }
        }
        if (!percentiles.isEmpty()) {
            Percentile percentileComputer = new Percentile();
            percentileComputer.setData(data);
            for (double percentile : percentiles) {
                double percentileValue = percentileComputer.evaluate(percentile * 100);
                resultSeries.addPercentile(percentileValue, time, tagValues, percentile);
            }
        }
    }

    @VisibleForTesting
    static String getPercentileTag(double percentile) {
        return PERCENTILE_TAG_FORMATTER.format(percentile);
    }

    private List<String> getTagsList(TagContext tagContext) {
        String[] tagValues = new String[tagIndices.size()];
        Arrays.fill(tagValues, "");
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
        return tagValues
                .stream()
                .map(LabelValue::create)
                .collect(Collectors.toList());
    }

    private List<LabelValue> toLabelValuesWithPercentile(List<String> tagValues, double percentile) {
        List<LabelValue> result = new ArrayList<>(toLabelValues(tagValues));
        result.add(LabelValue.create(getPercentileTag(percentile)));
        return result;
    }

    private List<LabelKey> getLabelKeysInOrderForPercentiles() {
        LabelKey[] keys = new LabelKey[tagIndices.size() + 1];
        tagIndices.forEach((tag, index) -> keys[index] = LabelKey.create(tag, ""));
        keys[keys.length - 1] = LabelKey.create(PERCENTILE_TAG_KEY, "");
        return Arrays.asList(keys);
    }

    private List<LabelKey> getLabelKeysInOrderForMinMax() {
        LabelKey[] keys = new LabelKey[tagIndices.size()];
        tagIndices.forEach((tag, index) -> keys[index] = LabelKey.create(tag, ""));
        return Arrays.asList(keys);
    }

    private long getInMillis(Timestamp time) {
        return Duration.ofSeconds(time.getSeconds()).toMillis() + Duration.ofNanos(time.getNanos()).toMillis();
    }

    private class ResultSeriesCollector {

        private List<TimeSeries> minSeries = new ArrayList<>();

        private List<TimeSeries> maxSeries = new ArrayList<>();

        private List<TimeSeries> percentileSeries = new ArrayList<>();

        void addMinimum(double value, Timestamp time, List<String> tags) {
            Point pt = Point.create(Value.doubleValue(value), time);
            minSeries.add(TimeSeries.createWithOnePoint(toLabelValues(tags), pt, time));
        }

        void addMaximum(double value, Timestamp time, List<String> tags) {
            Point pt = Point.create(Value.doubleValue(value), time);
            maxSeries.add(TimeSeries.createWithOnePoint(toLabelValues(tags), pt, time));
        }

        void addPercentile(double value, Timestamp time, List<String> tags, double percentile) {
            Point pt = Point.create(Value.doubleValue(value), time);
            percentileSeries.add(TimeSeries.createWithOnePoint(toLabelValuesWithPercentile(tags, percentile), pt, time));
        }
    }

}

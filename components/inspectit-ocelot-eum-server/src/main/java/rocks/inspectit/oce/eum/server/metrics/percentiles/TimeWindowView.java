package rocks.inspectit.oce.eum.server.metrics.percentiles;

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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * COPIED FROM THE OCELOT CORE PROJECT AND MODIFIED!
 * <p>
 * Holds the data for a given measurement splitted by a provided set of tags over a given time window.
 */
@Slf4j
public abstract class TimeWindowView {

    private static final Duration CLEANUP_INTERVAL = Duration.ofSeconds(1);

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
     * The timestamp when the last full cleanup happened.
     */
    private AtomicLong lastCleanupTimeMs;

    /**
     * Constructor.
     *
     * @param tags             the tags to use for this view
     * @param timeWindowMillis the time range in milliseconds to use for computing minimum / maximum and percentile values
     * @param viewName         the prefix to use for the names of all exposed metrics
     * @param unit             the unit of the measure
     * @param description      the description of this view
     * @param bufferLimit      the maximum number of measurements to be buffered by this view
     */
    TimeWindowView(Set<String> tags, long timeWindowMillis, String viewName, String unit, String description, int bufferLimit) {
        validateConfiguration(timeWindowMillis, viewName, unit, description, bufferLimit);
        assignTagIndices(tags);
        seriesValues = new ConcurrentHashMap<>();
        this.timeWindowMillis = timeWindowMillis;
        this.viewName = viewName;
        this.unit = unit;
        this.description = description;
        this.bufferLimit = bufferLimit;
        numberOfPoints = new AtomicInteger(0);
        lastCleanupTimeMs = new AtomicLong(0);
    }

    private void validateConfiguration(long timeWindowMillis, String baseViewName, String unit, String description, int bufferLimit) {
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
        removeStalePointsIfTimeThresholdExceeded(time);
        List<String> tags = getTagsList(tagContext);
        WindowedDoubleQueue queue = seriesValues.computeIfAbsent(tags, (key) -> new WindowedDoubleQueue(timeWindowMillis));
        synchronized (queue) {
            long timeMillis = getInMillis(time);
            int removed = queue.removeStaleValues(timeMillis);
            int currentSize = numberOfPoints.addAndGet(-removed);
            if (currentSize < bufferLimit) {
                numberOfPoints.incrementAndGet();
                queue.insert(value, timeMillis);
            } else {
                if (!overflowWarningPrinted) {
                    overflowWarningPrinted = true;
                    log.warn("Dropping points for Percentiles-View '{}' because the buffer limit has been reached!" + " Quantiles/Min/Max will be meaningless." + " This warning will not be shown for future drops!", viewName);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the name of the series exposed by this view.
     * This can be up to three series, depending on whether min/max and quantiles are enabled.
     *
     * @return the names of the exposed series.
     */
    abstract Set<String> getSeriesNames();

    /**
     * Removes all data which has fallen out of the time window based on the given timestamp.
     *
     * @param time the current time
     */
    private void removeStalePoints(Timestamp time) {
        long timeMillis = getInMillis(time);
        lastCleanupTimeMs.set(timeMillis);
        for (WindowedDoubleQueue queue : seriesValues.values()) {
            synchronized (queue) {
                int removed = queue.removeStaleValues(timeMillis);
                numberOfPoints.getAndAdd(-removed);
            }
        }
    }

    /**
     * Removes all data which has fallen out of the time window based on the given timestamp.
     * Only performs the cleanup if the last cleanup has been done more than {@link #CLEANUP_INTERVAL} ago
     * and the buffer is running on it's capacity limit.
     *
     * @param time the current time
     */
    private void removeStalePointsIfTimeThresholdExceeded(Timestamp time) {
        long timeMillis = getInMillis(time);
        long lastCleanupTime = lastCleanupTimeMs.get();
        boolean timeThresholdExceeded = timeMillis - lastCleanupTime > CLEANUP_INTERVAL.toMillis();
        if (timeThresholdExceeded && numberOfPoints.get() >= bufferLimit) {
            removeStalePoints(time);
        }
    }

    /**
     * @return the tags used for this view
     */
    Set<String> getTagKeys() {
        return tagIndices.keySet();
    }

    protected abstract List<MetricDescriptor> getMetrics();

    /**
     * Computes the defined percentile and min / max metrics.
     *
     * @param time the current timestamp
     *
     * @return the metrics containing the percentiles and min / max
     */
    Collection<Metric> computeMetrics(Timestamp time) {
        removeStalePoints(time);
        ResultSeriesCollector resultSeries = new ResultSeriesCollector(getMetrics());
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
        for (Map.Entry<MetricDescriptor, List<TimeSeries>> metric : resultSeries.seriesMap.entrySet()) {
            resultMetrics.add(Metric.create(metric.getKey(), metric.getValue()));
        }
        return resultMetrics;
    }

    protected abstract void computeSeries(List<String> tagValues, double[] data, Timestamp time, ResultSeriesCollector resultSeries);

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
        return tagValues.stream().map(LabelValue::create).collect(Collectors.toList());
    }

    protected List<LabelKey> getLabelKeysInOrder() {
        LabelKey[] keys = new LabelKey[tagIndices.size()];
        tagIndices.forEach((tag, index) -> keys[index] = LabelKey.create(tag, ""));
        return Arrays.asList(keys);
    }

    protected List<LabelKey> getLabelKeysInOrder(String extraTag) {
        LabelKey[] keys = new LabelKey[tagIndices.size() + 1];
        tagIndices.forEach((tag, index) -> keys[index] = LabelKey.create(tag, ""));
        keys[keys.length - 1] = LabelKey.create(extraTag, "");
        return Arrays.asList(keys);
    }

    private long getInMillis(Timestamp time) {
        return Duration.ofSeconds(time.getSeconds()).toMillis() + Duration.ofNanos(time.getNanos()).toMillis();
    }

    protected class ResultSeriesCollector {

        private Map<MetricDescriptor, List<TimeSeries>> seriesMap = new HashMap<>();

        public ResultSeriesCollector(List<MetricDescriptor> metrics) {
            if (metrics != null) {
                metrics.forEach(metric -> seriesMap.put(metric, new ArrayList<>()));
            }
        }

        void add(MetricDescriptor metric, double value, Timestamp time, List<String> tags) {
            List<TimeSeries> series = seriesMap.computeIfAbsent(metric, m -> new ArrayList<>());
            Point pt = Point.create(Value.doubleValue(value), time);
            series.add(TimeSeries.createWithOnePoint(toLabelValues(tags), pt, time));
        }

    }

}

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds the data for a given measurement splitted by a provided set of tags over a given time window.
 * For the data within this window, percentiles and min / max values can be computed.
 */
public class PercentileView {

    /**
     * The tag to use for the percentile or "min","max" respectively.
     */
    private static final String PERCENTILE_TAG_KEY = "p";

    /**
     * The tag value to use for {@link #PERCENTILE_TAG_KEY} for the "minimum" series.
     */
    private static final String MIN_TAG = "min";

    /**
     * The tag value to use for {@link #PERCENTILE_TAG_KEY} for the "maximum" series.
     */
    private static final String MAX_TAG = "max";

    /**
     * The formatter used to print percentiles to tags.
     */
    private static final DecimalFormat PERCENTILE_TAG_FORMATTER = new DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    /**
     * The descriptor of the metric for this view.
     */
    private MetricDescriptor metricDescriptor;

    /**
     * If true, the minimum value will be exposed as a gauge.
     */
    @Getter
    private boolean minEnabled;

    /**
     * If true, the maximum value will be exposed as a gauge.
     */
    @Getter
    private boolean maxEnabled;

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
     */
    PercentileView(boolean includeMin, boolean includeMax, Set<Double> percentiles, Set<String> tags, long timeWindowMillis, String viewName, String unit, String description) {
        validateConfiguration(includeMin, includeMax, percentiles, timeWindowMillis, viewName, unit, description);
        assignTagIndices(tags);
        seriesValues = new ConcurrentHashMap<>();
        this.timeWindowMillis = timeWindowMillis;
        this.viewName = viewName;
        this.unit = unit;
        this.description = description;
        minEnabled = includeMin;
        maxEnabled = includeMax;
        this.percentiles = new HashSet<>(percentiles);

        List<LabelKey> labelKeys = getLabelKeysInOrder();
        metricDescriptor = MetricDescriptor.create(viewName, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, labelKeys);
    }

    private void validateConfiguration(boolean includeMin, boolean includeMax, Set<Double> percentiles, long timeWindowMillis,
                                       String baseViewName, String unit, String description) {
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
    Metric computeMetrics(Timestamp time) {
        List<TimeSeries> resultSeries = new ArrayList<>();
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
                resultSeries.addAll(computeSeries(tagValues, data, time));
            }
        }
        return Metric.create(metricDescriptor, resultSeries);
    }

    private List<TimeSeries> computeSeries(List<String> tagValues, double[] data, Timestamp time) {
        List<TimeSeries> results = new ArrayList<>();
        if (isMinEnabled() || isMaxEnabled()) {
            double minValue = Double.MAX_VALUE;
            double maxValue = -Double.MAX_VALUE;
            for (double value : data) {
                minValue = Math.min(minValue, value);
                maxValue = Math.max(maxValue, value);
            }
            if (isMinEnabled()) {
                results.add(createTimeSeries(time, minValue, tagValues, MIN_TAG));
            }
            if (isMaxEnabled()) {
                results.add(createTimeSeries(time, maxValue, tagValues, MAX_TAG));
            }
        }
        if (!percentiles.isEmpty()) {
            Percentile percentileComputer = new Percentile();
            percentileComputer.setData(data);
            for (double percentile : percentiles) {
                double percentileValue = percentileComputer.evaluate(percentile * 100);
                String percentileTag = getPercentileTag(percentile);
                results.add(createTimeSeries(time, percentileValue, tagValues, percentileTag));
            }
        }
        return results;
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

    private List<LabelValue> toLabelValues(List<String> tagValues, String percentileTag) {
        return Stream.concat(
                tagValues
                        .stream()
                        .map(LabelValue::create),
                Stream.of(LabelValue.create(percentileTag))
        ).collect(Collectors.toList());
    }

    private List<LabelKey> getLabelKeysInOrder() {
        LabelKey[] keys = new LabelKey[tagIndices.size() + 1];
        tagIndices.forEach((tag, index) -> keys[index] = LabelKey.create(tag, ""));
        keys[keys.length - 1] = LabelKey.create(PERCENTILE_TAG_KEY, "");
        return Arrays.asList(keys);
    }

    private long getInMillis(Timestamp time) {
        return Duration.ofSeconds(time.getSeconds()).toMillis() + Duration.ofNanos(time.getNanos()).toMillis();
    }

    private TimeSeries createTimeSeries(Timestamp time, double value, List<String> tags, String percentileTag) {
        Point point = Point.create(Value.doubleValue(value), time);
        List<LabelValue> labelValues = toLabelValues(tags, percentileTag);
        return TimeSeries.createWithOnePoint(labelValues, point, time);
    }

}

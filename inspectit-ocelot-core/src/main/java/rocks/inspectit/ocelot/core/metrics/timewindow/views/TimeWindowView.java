package rocks.inspectit.ocelot.core.metrics.timewindow.views;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.resources.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds the data for a given measurement split by a provided set of attributes over a given time window.
 */
@Slf4j
public abstract class TimeWindowView {

    private static final Duration CLEANUP_INTERVAL = Duration.ofSeconds(1);

    /**
     * Stores the buffered data of the sliding time window for each
     * time series (unique combination of attribute values).
     */
    private final ConcurrentHashMap<List<String>, WindowedDoubleQueue> seriesValues =  new ConcurrentHashMap<>();

    /**
     * The current number of points stored in this view, limited by {@link #bufferLimit}.
     */
    private final AtomicInteger numberOfPoints = new AtomicInteger(0);

    /**
     * The timestamp when the last full cleanup happened.
     */
    private final AtomicLong lastCleanupTimeMs = new AtomicLong(0);

    /**
     * Defines the attributes which are used for the view. Only these attributes can be recorded for the view!
     * E.g. if the attribute "http_path" is used, quantiles will be computed for each http_path individually.
     * <p>
     * The attribute values are stored in a fixed order in the keys of {@link #seriesValues} for each series.
     * The values here define at which position within these arrays the corresponding attribute value is found.
     * E.g. if tagIndex["http_path"] = 2, this means that the values for http_path will be at index 2 in the keys of {@link #seriesValues}.
     */
    private final Map<String, Integer> attributeIndices = new LinkedHashMap<>();

    /**
     * The name of the view, used as prefix for all individual metrics.
     */
    @Getter
    private final String viewName;

    /**
     * The description of this view.
     */
    @Getter
    protected final String description;

    /**
     * The unit of the metric.
     */
    @Getter
    protected final String unit;

    /**
     * Defines the size of the sliding window.
     */
    @Getter
    protected final Duration timeWindow;

    /**
     * The maximum amount of measurement points to buffer.
     * If this limit is reached, new metrics will be rejected until there is space again.
     */
    @Getter
    protected final int bufferLimit;

    private boolean overflowWarningPrinted = false;

    /**
     * @param viewName         the prefix to use for the names of all exposed metrics
     * @param description      the description of this view
     * @param unit             the unit of the measure
     * @param attributes       the attribute keys to use for this view
     * @param timeWindow       the time range to use for computing minimum / maximum and quantiles values
     * @param bufferLimit      the maximum number of measurements to be buffered by this view
     */
    TimeWindowView(String viewName, String description, String unit, Set<String> attributes, Duration timeWindow, int bufferLimit) {
        validateConfiguration(timeWindow, viewName, unit, description, bufferLimit);
        assignAttributeIndices(attributes);
        this.timeWindow = timeWindow;
        this.viewName = viewName;
        this.unit = unit;
        this.description = description;
        this.bufferLimit = bufferLimit;
    }

    private void validateConfiguration(Duration timeWindow, String baseViewName, String unit, String description, int bufferLimit) {
        if (StringUtils.isBlank(baseViewName)) {
            throw new IllegalArgumentException("View name must not be blank!");
        }
        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("Description must not be blank!");
        }
        if (StringUtils.isBlank(unit)) {
            throw new IllegalArgumentException("Unit must not be blank!");
        }
        if (timeWindow.toMillis() <= 0) {
            throw new IllegalArgumentException("Time window must be positive!");
        }
        if (bufferLimit < 1) {
            throw new IllegalArgumentException("The buffer limit must be greater than or equal to 1!");
        }
    }

    private void assignAttributeIndices(Collection<String> attributes) {
        int idx = 0;
        for (String attribute : attributes) {
            attributeIndices.put(attribute, idx);
            idx++;
        }
    }

    /**
     * @return the accepted attribute keys
     */
    public Set<String> getAttributeKeys() {
        return attributeIndices.keySet();
    }

    /**
     * Adds the provided value to the sliding window of data.
     *
     * @param value      the value of the metric
     * @param time       the timestamp when this value was observed
     * @param baggage    the baggage with of the observed value
     *
     * @return true, if the point could be added, false otherwise
     */
    public boolean insertValue(double value, Instant time, Baggage baggage) {
        removeStalePointsIfTimeThresholdExceeded(time);
        List<String> attributeValues = getAttributeValuesInOrder(baggage);
        WindowedDoubleQueue queue = seriesValues.computeIfAbsent(attributeValues, (v) -> new WindowedDoubleQueue(timeWindow));
        synchronized (queue) {
            long timeMillis = time.toEpochMilli();
            int removed = queue.removeStaleValues(timeMillis);
            int currentSize = numberOfPoints.addAndGet(-removed);
            if (currentSize < bufferLimit) {
                numberOfPoints.incrementAndGet();
                queue.insert(value, timeMillis);
            } else {
                if (!overflowWarningPrinted) {
                    overflowWarningPrinted = true;
                    log.warn("Dropping points for Quantiles-View '{}' because the buffer limit has been reached!" + " Quantiles/Min/Max will be meaningless." + " This warning will not be shown for future drops!", viewName);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Removes all data which has fallen out of the time window based on the given timestamp.
     *
     * @param time the current time
     */
    private void removeStalePoints(Instant time) {
        long timeMillis = time.toEpochMilli();
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
     * and the buffer is running on its capacity limit.
     *
     * @param time the current time
     */
    private void removeStalePointsIfTimeThresholdExceeded(Instant time) {
        long timeMillis = time.toEpochMilli();
        long lastCleanupTime = lastCleanupTimeMs.get();
        boolean timeThresholdExceeded = timeMillis - lastCleanupTime > CLEANUP_INTERVAL.toMillis();
        if (timeThresholdExceeded && numberOfPoints.get() >= bufferLimit) {
            removeStalePoints(time);
        }
    }

    /**
     * Creates a list of ordered attribute values. The positions are derived from {@link #attributeIndices}.
     *
     * @return the attribute values used for this view
     */
    private List<String> getAttributeValuesInOrder(Baggage baggage) {
        String[] orderedValues = new String[attributeIndices.size()];

        baggage.forEach((key, value) -> {
            if (attributeIndices.containsKey(key)) { // Only care about baggage which is configured for the view
                int idx = attributeIndices.get(key);
                orderedValues[idx] = value.getValue();
            }
        });

        return Arrays.asList(orderedValues);
    }

    protected abstract Collection<MetricInfo> getMetrics();

    /**
     * Computes the defined quantiles and min / max metrics.
     *
     * @param time the current timestamp
     *
     * @return the metrics containing the quantiles and min / max
     */
    public Collection<MetricData> computeMetrics(Instant time, Resource resource) {
        removeStalePoints(time);
        ResultSeriesCollector resultSeries = ResultSeriesCollector.create(getMetrics());
        for (Map.Entry<List<String>, WindowedDoubleQueue> series : seriesValues.entrySet()) {
            List<String> attributeValues = series.getKey();
            Attributes attributes = buildAttributes(attributeValues);
            WindowedDoubleQueue queue = series.getValue();
            double[] data = null;
            synchronized (queue) {
                int size = queue.size();
                if (size > 0) {
                    data = queue.copy();
                }
            }
            if (data != null) {
                computeSeries(resultSeries, time, attributes, data);
            }
        }

        List<MetricData> resultMetrics = new ArrayList<>();
        for (Map.Entry<MetricInfo, Collection<DoublePointData>> entry : resultSeries.seriesMap.entrySet()) {
            MetricData metricData = createMetricData(resource, entry.getKey(), entry.getValue());
            resultMetrics.add(metricData);
        }
        return resultMetrics;
    }

    /**
     * Builds attributes from keys in {@link #attributeIndices} and the provided values.
     * We expect the values to be properly ordered.
     *
     * @param attributeValues the ordered attribute values
     *
     * @return the attribute keys and values
     */
    private Attributes buildAttributes(List<String> attributeValues) {
        Iterator<String> keyIterator = attributeIndices.keySet().iterator();
        Iterator<String> valueIterator = attributeValues.iterator();
        AttributesBuilder builder = Attributes.builder();

        while (keyIterator.hasNext() && valueIterator.hasNext()) {
            builder.put(keyIterator.next(), valueIterator.next());
        }

        return builder.build();
    }

    /**
     * Creates the metric data object. At the moment all time-window views only support {@link MetricDataType#DOUBLE_GAUGE}.
     *
     * @param resource the resource information
     * @param metricInfo the metric information
     * @param pointData the data points
     *
     * @return the created metric data
     */
    private MetricData createMetricData(Resource resource, MetricInfo metricInfo, Collection<DoublePointData> pointData) {
        return ImmutableMetricData.createDoubleGauge(
                resource,
                OpenTelemetryUtils.DEFAULT_INSTRUMENTATION_SCOPE_INFO,
                metricInfo.name,
                metricInfo.description,
                metricInfo.unit,
                ImmutableGaugeData.create(pointData)
        );
    }

    /**
     * Computes the data into one series.
     *
     * @param resultSeries the resulting time series
     * @param time         the timestamp
     * @param attributes   the attributes for the time series
     * @param data         the series data
     */
    protected abstract void computeSeries(ResultSeriesCollector resultSeries, Instant time, Attributes attributes, double[] data);

    @AllArgsConstructor
    protected static class MetricInfo {
        private final String name;

        private final String description;

        private final String unit;
    }

    protected static class ResultSeriesCollector {

        private final Map<MetricInfo, Collection<DoublePointData>> seriesMap = new HashMap<>();

        public static ResultSeriesCollector create(Collection<MetricInfo> metrics) {
            ResultSeriesCollector resultSeries = new ResultSeriesCollector();
            if (!CollectionUtils.isEmpty(metrics)) {
                metrics.forEach(metric -> resultSeries.seriesMap.put(metric, new ArrayList<>()));
            }
            return resultSeries;
        }

        /**
         * Creates and adds a data point to the series
         *
         * @param metric the metric information
         * @param time the time of recording the data point
         * @param attributes the data attributes
         * @param value the data value
         */
        void add(MetricInfo metric, Instant time, Attributes attributes, double value) {
            Collection<DoublePointData> series = seriesMap.get(metric);
            DoublePointData pointData = createPointData(time, attributes, value);
            series.add(pointData);
        }

        private DoublePointData createPointData(Instant time, Attributes attributes, double value) {
            long timestamp = time.toEpochMilli() * 1_000_000 + time.getNano();
            return ImmutableDoublePointData.create(
                    timestamp,
                    timestamp,
                    attributes,
                    value
            );
        }
    }
}

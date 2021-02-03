package rocks.inspectit.oce.eum.server.metrics.percentiles;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.common.Timestamp;
import io.opencensus.metrics.Metrics;
import io.opencensus.metrics.export.Metric;
import io.opencensus.metrics.export.MetricProducer;
import io.opencensus.stats.MeasureMap;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.Tags;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * COPIED FROM THE OCELOT CORE PROJECT!
 * <p>
 * Allows the creation, update and removal of percentile views on metrics.
 * Note that these views coexist to opencensus {@link io.opencensus.stats.View}s.
 * For this reason observation must be reported via {@link #recordMeasurement(String, double)}
 * in addition to {@link MeasureMap#record()}.
 */
@Component
public class TimeWindowViewManager {

    /**
     * Maps the name of measures to registered percentile views.
     */
    private ConcurrentHashMap<String, CopyOnWriteArrayList<TimeWindowView>> measuresToViewsMap = new ConcurrentHashMap<>();

    /**
     * Computation of percentiles can be expensive.
     * For this reason we cache computed metrics for 1 second before recomputing them.
     * Otherwise e.g. spamming F5 on the prometheus endpoint could lead to an increased CPU usage.
     */
    private final MetricProducer producer = new CachingMetricProducer(this::computeMetrics, Duration.ofSeconds(1));

    /**
     * The clock used for timing metrics.
     */
    private final Supplier<Long> clock;

    /**
     * Maps series names to the corresponding names of measures.
     * Acts as a cache which is invalidated when views are added changed or removed.
     */
    private Map<String, String> seriesToMeasuresCache;

    /**
     * Recording observation takes amortized O(1) time.
     * However, the worst-case time of a recording is O(n), which is why we decouple the recording from the application threads.
     * This worker maintains a fixed-size queue of observations which are then added via {@link #recordSynchronous(String, double, Timestamp, TagContext)}.
     */
    @VisibleForTesting
    AsyncMetricRecorder worker = new AsyncMetricRecorder(this::recordSynchronous);

    public TimeWindowViewManager() {
        this(System::currentTimeMillis);
    }

    @VisibleForTesting
    TimeWindowViewManager(Supplier<Long> clock) {
        this.clock = clock;
    }

    @PostConstruct
    void init() {
        Metrics.getExportComponent().getMetricProducerManager().add(producer);
    }

    @PreDestroy
    void destroy() {
        worker.destroy();
        Metrics.getExportComponent().getMetricProducerManager().remove(producer);
    }

    /**
     * Records a measurement observation for a given measure.
     * Tags are expected to be given via teh OC TagContext.
     *
     * @param measureName the name of the measure, e.g. http/responsetime
     * @param value       the observation to record
     */
    public void recordMeasurement(String measureName, double value) {
        recordMeasurement(measureName, value, Tags.getTagger().getCurrentTagContext());
    }

    /**
     * Records a measurement observation for a given measure.
     *
     * @param measureName the name of the measure, e.g. http/responsetime
     * @param value       the observation to record
     * @param tags        the TagContext to use
     */
    public void recordMeasurement(String measureName, double value, TagContext tags) {
        if (areAnyViewsRegisteredForMeasure(measureName)) {
            synchronized (this) {
                worker.record(measureName, value, getCurrentTime(), tags);
            }
        }
    }

    /**
     * Creates a new smoothed_average view if no view with the given name exists for the given measure.
     * If a view with the given name already exists for the given measure, it is updated instead.
     * When a view is updated, all buffered observation are lost.
     *
     * @param measureName      the name of the measure, e.g. "http/responsetime"
     * @param viewName         the name of the view, e.g. "http/responsetime/distribution"
     * @param unit             the unit of the view
     * @param description      the description for the view
     * @param dropUpper        value in percentage in the range (0,1) which indicates how many metrics in the upper range shall be dropped
     * @param dropLower        value in percentage in the range (0,1) which indicates how many metrics in the lower range shall be dropped
     * @param timeWindowMillis the length of the sliding time window to use for computing min / max and the percentiles
     * @param tags             the tags to use for the view
     * @param bufferLimit      the maximum number of points this view is allowed to buffer
     */
    public void createOrUpdateSmoothedAverageView(String measureName, String viewName, String unit, String description, double dropUpper, double dropLower, long timeWindowMillis, Collection<String> tags, int bufferLimit) {
        createOrUpdateView(SmoothedAverageView.class, measureName, viewName, unit, description, false, false, null, dropUpper, dropLower, timeWindowMillis, tags, bufferLimit);
    }

    /**
     * Creates a new percentile view if no view with the given name exists for the given measure.
     * If a view with the given name already exists for the given measure, it is updated instead.
     * When a view is updated, all buffered observation are lost.
     *
     * @param measureName      the name of the measure, e.g. "http/responsetime"
     * @param viewName         the name of the view, e.g. "http/responsetime/distribution"
     * @param unit             the unit of the view
     * @param description      the description for the view
     * @param minEnabled       true, if the minimum shall be exposed as metric
     * @param maxEnabled       true, if the minimum shall be exposed as metric
     * @param percentiles      specified which percentiles shall be exposed as metric, values are in the range (0,1)
     * @param timeWindowMillis the length of the sliding time window to use for computing min / max and the percentiles
     * @param tags             the tags to use for the view
     * @param bufferLimit      the maximum number of points this view is allowed to buffer
     */
    public void createOrUpdatePercentileView(String measureName, String viewName, String unit, String description, boolean minEnabled, boolean maxEnabled, Collection<Double> percentiles, long timeWindowMillis, Collection<String> tags, int bufferLimit) {
        createOrUpdateView(PercentileView.class, measureName, viewName, unit, description, minEnabled, maxEnabled, percentiles, -1, -1, timeWindowMillis, tags, bufferLimit);
    }

    private synchronized <T extends TimeWindowView> void createOrUpdateView(Class<T> viewType, String measureName, String viewName, String unit, String description, boolean minEnabled, boolean maxEnabled, Collection<Double> percentiles, double dropUpper, double dropLower, long timeWindowMillis, Collection<String> tags, int bufferLimit) {
        List<TimeWindowView> views = measuresToViewsMap.computeIfAbsent(measureName, (name) -> new CopyOnWriteArrayList<>());
        Optional<TimeWindowView> existingView = views.stream()
                .filter(view -> view.getViewName().equalsIgnoreCase(viewName))
                .findFirst();
        Optional<T> updatedView;
        if (existingView.isPresent()) {
            updatedView = updateView(viewType, existingView.get(), unit, description, minEnabled, maxEnabled, percentiles, dropUpper, dropLower, timeWindowMillis, tags, bufferLimit);
        } else {
            updatedView = Optional.of(createView(viewType, viewName, unit, description, minEnabled, maxEnabled, percentiles, dropUpper, dropLower, timeWindowMillis, tags, bufferLimit));
        }
        if (updatedView.isPresent()) {
            existingView.ifPresent(views::remove);
            views.add(updatedView.get());
        }
        seriesToMeasuresCache = null;
    }

    public synchronized boolean isViewRegistered(String measureName, String viewName) {
        List<TimeWindowView> views = measuresToViewsMap.get(measureName);
        if (views != null) {
            return views.stream().map(TimeWindowView::getViewName).anyMatch(name -> name.equals(viewName));
        }
        return false;
    }

    /**
     * Each percentile view can expose a set of series.
     * Given the name of such a series, this method returns the measure for which the corresponding view is registered.
     *
     * @param seriesName the name of the series
     *
     * @return the name of the source measure or null if this series does not originate from a percentile view.
     */
    public String getMeasureNameForSeries(String seriesName) {
        return getSeriesToMeasuresCache().get(seriesName);
    }

    /**
     * Removes the given view from the given measure, if it exists.
     *
     * @param measureName the name of the measure
     * @param viewName    the name of the view
     *
     * @return true, if the view existed and has been removed, false otherwise
     */
    public synchronized boolean removeView(String measureName, String viewName) {
        List<TimeWindowView> views = measuresToViewsMap.get(measureName);
        if (views != null) {
            Optional<TimeWindowView> existingView = views.stream()
                    .filter(view -> view.getViewName().equalsIgnoreCase(viewName))
                    .findFirst();
            if (existingView.isPresent()) {
                views.remove(existingView.get());
                if (views.isEmpty()) {
                    measuresToViewsMap.remove(measureName);
                }
                seriesToMeasuresCache = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true, if any percentile view is registered for the given measure.
     *
     * @param measureName the name of the measure to check
     *
     * @return true, if any percentile views exist
     */
    @VisibleForTesting
    boolean areAnyViewsRegisteredForMeasure(String measureName) {
        return measuresToViewsMap.containsKey(measureName);
    }

    /**
     * Synchronously records the specified measure observation.
     *
     * @param measure    the name of the measure
     * @param value      the observed value
     * @param time       the timestamp of the observation
     * @param tagContext the tag context of the observation
     */
    private void recordSynchronous(String measure, double value, Timestamp time, TagContext tagContext) {
        List<TimeWindowView> views = measuresToViewsMap.get(measure);
        if (views != null) {
            views.forEach(view -> view.insertValue(value, time, tagContext));
        }
    }

    private synchronized Map<String, String> getSeriesToMeasuresCache() {
        if (seriesToMeasuresCache == null) {
            seriesToMeasuresCache = new HashMap<>();
            measuresToViewsMap.forEach((measure, views) -> views.stream()
                    .flatMap(view -> view.getSeriesNames().stream())
                    .forEach(series -> seriesToMeasuresCache.put(series, measure)));
        }
        return seriesToMeasuresCache;
    }

    /**
     * @return the current time as OC timestamp.
     */
    private Timestamp getCurrentTime() {
        return Timestamp.fromMillis(clock.get());
    }

    private <T extends TimeWindowView> Optional<T> updateView(Class<T> viewType, TimeWindowView existingView, String unit, String description, boolean minEnabled, boolean maxEnabled, Collection<Double> percentiles, double dropUpper, double dropLower, long timeWindowMillis, Collection<String> tags, int bufferLimit) {
        Supplier<T> creator = () -> createView(viewType, existingView.getViewName(), unit, description, minEnabled, maxEnabled, percentiles, dropUpper, dropLower, timeWindowMillis, tags, bufferLimit);

        if (!unit.equals(existingView.getUnit())) {
            return Optional.of(creator.get());
        }
        if (!description.equals(existingView.getDescription())) {
            return Optional.of(creator.get());
        }
        if (timeWindowMillis != existingView.getTimeWindowMillis()) {
            return Optional.of(creator.get());
        }
        if (!existingView.getTagKeys().equals(new HashSet<>(tags))) {
            return Optional.of(creator.get());
        }
        if (existingView.getBufferLimit() != bufferLimit) {
            return Optional.of(creator.get());
        }

        if (existingView instanceof PercentileView) {
            PercentileView percentileView = (PercentileView) existingView;
            if (minEnabled != percentileView.isMinEnabled()) {
                return Optional.of(creator.get());
            }
            if (maxEnabled != percentileView.isMaxEnabled()) {
                return Optional.of(creator.get());
            }
            if (!percentileView.getPercentiles().equals(new HashSet<>(percentiles))) {
                return Optional.of(creator.get());
            }
        }

        if (existingView instanceof SmoothedAverageView) {
            SmoothedAverageView saView = (SmoothedAverageView) existingView;

            if (dropUpper != saView.getDropUpper()) {
                return Optional.of(creator.get());
            }
            if (dropLower != saView.getDropLower()) {
                return Optional.of(creator.get());
            }
        }

        return Optional.empty();
    }

    private <T extends TimeWindowView> T createView(Class<T> viewType, String viewName, String unit, String description, boolean minEnabled, boolean maxEnabled, Collection<Double> percentiles, double dropUpper, double dropLower, long timeWindowMillis, Collection<String> tags, int bufferLimit) {
        if (PercentileView.class.equals(viewType)) {
            return (T) new PercentileView(minEnabled, maxEnabled, new HashSet<>(percentiles), new HashSet<>(tags), timeWindowMillis, viewName, unit, description, bufferLimit);
        } else {
            return (T) new SmoothedAverageView(dropUpper, dropLower, new HashSet<>(tags), timeWindowMillis, viewName, unit, description, bufferLimit);
        }
    }

    @VisibleForTesting
    Collection<Metric> computeMetrics() {
        Timestamp now = getCurrentTime();
        return measuresToViewsMap.values()
                .stream()
                .flatMap(Collection::stream)
                .flatMap(view -> view.computeMetrics(now).stream())
                .collect(Collectors.toList());
    }

}

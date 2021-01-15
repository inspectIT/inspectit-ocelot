package rocks.inspectit.oce.eum.server.metrics.percentiles;

import io.opencensus.metrics.export.Metric;
import io.opencensus.metrics.export.MetricProducer;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * COPIED FROM THE OCELOT CORE PROJECT!
 * <p>
 * A metric producer which caches the metrics for a specified amount of time.
 */
public class CachingMetricProducer extends MetricProducer {

    /**
     * The function invoked to generate the metrics.
     */
    private final Supplier<Collection<Metric>> computeMetricsFunction;

    /**
     * The duration for which cached metrics are kept.
     */
    private final long cacheDurationNanos;

    /**
     * The timestamp when the metrics were computed the last time.
     */
    private long cacheTimestamp;

    private Collection<Metric> cachedMetrics = null;

    /**
     * Constructor.
     *
     * @param computeMetricsFunction the function to invoke for computing the metrics
     * @param cacheDuration          the duration for which the values shall be cached.
     */
    public CachingMetricProducer(Supplier<Collection<Metric>> computeMetricsFunction, Duration cacheDuration) {
        this.computeMetricsFunction = computeMetricsFunction;
        cacheDurationNanos = cacheDuration.toNanos();
    }

    @Override
    public synchronized Collection<Metric> getMetrics() {
        long now = System.nanoTime();
        if (cachedMetrics == null || (now - cacheTimestamp) > cacheDurationNanos) {
            cachedMetrics = computeMetricsFunction.get();
            cacheTimestamp = now;
        }
        return cachedMetrics;
    }
}

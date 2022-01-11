package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A custom {@link SpanExporter} wrapper that can be dynamically enabled or disabled.
 */
@Data
@Slf4j
public class DynamicallyActivatableSpanExporter<T extends SpanExporter> implements SpanExporter {

    ReadWriteLock lock = new ReentrantReadWriteLock();

    @Setter(AccessLevel.PRIVATE)
    private boolean enabled;

    /**
     * The underlying implementation of the {@link SpanExporter}
     */
    private T exporter;

    private DynamicallyActivatableSpanExporter(T spanExporter) {
        exporter = spanExporter;
    }

    private Map<String, Handler> serviceHandlers = new ConcurrentHashMap<>();

    /**
     * Creates a new {@link DynamicallyActivatableSpanExporter} with the given {@link SpanExporter}s as the implemented exporter
     *
     * @param spanExporters
     *
     * @return {@link DynamicallyActivatableSpanExporter} with the given {@link SpanExporter}s as the implemented exporter
     */
    public static DynamicallyActivatableSpanExporter create(SpanExporter... spanExporters) {
        return new DynamicallyActivatableSpanExporter(SpanExporter.composite(spanExporters));
    }

    /**
     * Creates a new {@link DynamicallyActivatableSpanExporter} with a {@link LoggingSpanExporter} as the implemented {@link SpanExporter}
     *
     * @return {@link DynamicallyActivatableSpanExporter} with a {@link LoggingSpanExporter} as the implemented {@link SpanExporter}
     */
    public static DynamicallyActivatableSpanExporter<LoggingSpanExporter> createLoggingSpanExporter() {
        return new DynamicallyActivatableSpanExporter<>(new LoggingSpanExporter());
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // if enabled, call the real exporter's export method
        if (isEnabled()) {
            for (Handler handler : serviceHandlers.values()) {
                handler.export(spans);
            }
            lock.readLock().lock();
            try {
                return exporter.export(spans);
            } finally {
                lock.readLock().unlock();
            }
        }
        // otherwise, do nothing and return success
        else {
            return CompletableResultCode.ofSuccess();
        }
    }

    @Override
    public CompletableResultCode flush() {
        // TODO: implement own flush method?? , e.g.,
        lock.readLock().lock();
        try {
            return exporter.flush();
        } catch (Exception e) {
            log.error("failed to flush " + exporter.getClass(), e);
            return CompletableResultCode.ofFailure();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        doDisable();
        lock.readLock().lock();
        try {
            return exporter.shutdown();
        } catch (Exception e) {
            log.error("failed to shutdown " + exporter.getClass(), e);
            return CompletableResultCode.ofFailure();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean doEnable() {
        setEnabled(true);
        return true;
    }

    public boolean doDisable() {
        setEnabled(false);
        return true;
    }

    /**
     * Sets the underlying {@link #exporter}.
     * If an active {@link SpanExporter} was previously set, it will be shut down.
     *
     * @param exporter
     */
    public void setExporter(T exporter) {
        lock.writeLock().lock();
        try {
            this.exporter.flush();
            CompletableResultCode resultCode = this.exporter.shutdown();
            // TODO: do we need to wait for shutdown to complete?
        } catch (Exception e) {
            log.error("failed to shut down or flush exporter " + exporter.getClass() + " before setting new exporter", e);
        }
        this.exporter = exporter;
        lock.writeLock().unlock();
    }

    /**
     * Registers a new service handlers that is used to export {@link SpanData} for sampled {@link io.opentelemetry.api.trace.Span}s
     *
     * @param name           the name of the service handler. Must be unique for each service.
     * @param serviceHandler the service handler that is called for each ended sampled {@link io.opentelemetry.api.trace.Span}
     */
    public void registerHandler(String name, Handler serviceHandler) {
        serviceHandlers.put(name, serviceHandler);
    }

    /**
     * Unregisters the service handler with the provided name.
     *
     * @param name the name of the service handler that will be unregistered
     */
    public void unregisterHandler(String name) {
        serviceHandlers.remove(name);
    }

    /**
     * Mirror of {@link io.opencensus.trace.export.SpanExporter.Handler}.
     * An abstract class that allows different tracing services to export recorded data for sampled spans in their own format.
     */
    public abstract class Handler {

        /**
         * Exports a list of sampled {@link io.opentelemetry.api.trace.Span}s using the immutable representation {@link SpanData}.
         *
         * @param spanDataList a list of {@link SpanData} objects to be exported.
         */
        public abstract void export(Collection<SpanData> spanDataList);

    }

}

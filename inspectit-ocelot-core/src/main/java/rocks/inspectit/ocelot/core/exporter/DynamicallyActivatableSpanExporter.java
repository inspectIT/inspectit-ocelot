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

/**
 * A custom {@link SpanExporter} wrapper that can be dynamically enabled or disabled.
 */
@Data
@Slf4j
public class DynamicallyActivatableSpanExporter<T extends SpanExporter> implements SpanExporter {

    @Setter(AccessLevel.PRIVATE)
    private boolean enabled;

    /**
     * The underlying implementation of the {@link SpanExporter}
     */
    private T exporter;

    private DynamicallyActivatableSpanExporter(T spanExporter) {
        exporter = spanExporter;
    }

    /**
     * Creates a new {@link DynamicallyActivatableSpanExporter} with the given {@link SpanExporter} as the implemented exporter
     *
     * @param spanExporter
     *
     * @return {@link DynamicallyActivatableSpanExporter} with the given {@link SpanExporter} as the implemented exporter
     */
    public static DynamicallyActivatableSpanExporter create(SpanExporter spanExporter) {
        return new DynamicallyActivatableSpanExporter(spanExporter);
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
            return exporter.export(spans);
        }
        // otherwise, do nothing and return success
        else {
            return CompletableResultCode.ofSuccess();
        }
    }

    @Override
    public CompletableResultCode flush() {
        return exporter.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return exporter.shutdown();
    }

    public boolean doEnable() {
        setEnabled(true);
        return true;
    }

    public boolean doDisable() {
        setEnabled(false);
        return true;
    }

}

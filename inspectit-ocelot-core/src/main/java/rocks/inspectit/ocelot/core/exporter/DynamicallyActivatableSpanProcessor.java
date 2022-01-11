package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A custom {@link SpanProcessor} wrapper that can be dynamically enabled or disabled.
 */
public class DynamicallyActivatableSpanProcessor implements SpanProcessor {

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private boolean enabled;

    /**
     * The real implementation of the {@link SpanProcessor}
     */
    private SpanProcessor spanProcessor;

    private Map<String, SpanProcessor> spanProcessors = new ConcurrentHashMap<>();

    private DynamicallyActivatableSpanProcessor(SpanProcessor spanProcessor) {
        this.spanProcessor = spanProcessor;
    }

    /**
     * Creates a new {@link DynamicallyActivatableSpanProcessor} with the given {@link SpanProcessor} as the implementation to process spans.
     *
     * @param spanProcessor The {@link SpanProcessor} that will be used to process the spans
     *
     * @return A new {@link DynamicallyActivatableSpanProcessor} with the given {@link SpanProcessor} as the implementation to process spans.
     */
    public static DynamicallyActivatableSpanProcessor create(SpanProcessor spanProcessor) {
        return new DynamicallyActivatableSpanProcessor(spanProcessor);
    }

    public static DynamicallyActivatableSpanProcessor createSimpleSpanProcessor(SpanExporter spanExporter) {
        return new DynamicallyActivatableSpanProcessor(SimpleSpanProcessor.create(spanExporter));
    }

    public static DynamicallyActivatableSpanProcessor createBatchSpanProcessor(SpanExporter... spanExporters) {
        return new DynamicallyActivatableSpanProcessor(BatchSpanProcessor.builder(SpanExporter.composite(spanExporters))
                .build());
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // if enabled, call the real processor's onStart
        if (isEnabled()) {
            if (null != spanProcessors) {
                for (SpanProcessor spanProc : spanProcessors.values()) {
                    spanProc.onStart(parentContext, span);
                }
            }
            spanProcessor.onStart(parentContext, span);
        }
        // otherwise, do nothing
    }

    @Override
    public boolean isStartRequired() {
        return isEnabled() ? spanProcessor.isStartRequired() : false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // if enabled, call the real processor's onEnd
        if (isEnabled()) {
            if (null != spanProcessors) {
                for (SpanProcessor spanProc : spanProcessors.values()) {
                    spanProc.onEnd(span);
                }
            }
            spanProcessor.onEnd(span);
        }
        // otherwise, do nothing
    }

    @Override
    public boolean isEndRequired() {
        return isEnabled() ? spanProcessor.isEndRequired() : false;
    }

    public boolean doEnable() {
        setEnabled(true);
        return true;
    }

    public boolean doDisable() {
        setEnabled(false);
        return true;
    }

    public void registerService(String registerName, SpanProcessor spanProcessor) {
        spanProcessors.put(registerName, spanProcessor);
    }

    public void unregisterService(String registerName) {
        spanProcessors.remove(registerName);
    }
}

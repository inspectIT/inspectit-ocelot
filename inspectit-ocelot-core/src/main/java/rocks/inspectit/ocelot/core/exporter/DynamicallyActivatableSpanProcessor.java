package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.Data;

/**
 * A custom {@link SpanProcessor} wrapper that can be dynamically enabled or disabled.
 */
@Data
public class DynamicallyActivatableSpanProcessor implements SpanProcessor {

    private boolean enabled;

    /**
     * The real implementation of the {@link SpanProcessor}
     */
    private SpanProcessor spanProcessor;

    private DynamicallyActivatableSpanProcessor(SpanProcessor spanProcessor) {
        this.spanProcessor = spanProcessor;
    }

    public static DynamicallyActivatableSpanProcessor create(SpanProcessor spanProcessor) {
        return new DynamicallyActivatableSpanProcessor(spanProcessor);
    }

    public static DynamicallyActivatableSpanProcessor createSimpleSpanProcessor(SpanExporter spanExporter) {
        return new DynamicallyActivatableSpanProcessor(SimpleSpanProcessor.create(spanExporter));
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // if enabled, call the real processor's onStart
        if (isEnabled()) {
            spanProcessor.onStart(parentContext, span);
        }
        // otherwise, do nothing
    }

    @Override
    public boolean isStartRequired() {
        return spanProcessor.isStartRequired();
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // if enabled, call the real processor's onEnd
        if (isEnabled()) {
            spanProcessor.onEnd(span);
        }
        // otherwise, do nothing
    }

    @Override
    public boolean isEndRequired() {
        return spanProcessor.isEndRequired();
    }
}

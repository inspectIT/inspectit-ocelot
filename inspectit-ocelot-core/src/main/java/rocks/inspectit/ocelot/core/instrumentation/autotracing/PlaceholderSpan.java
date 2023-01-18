package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.IdGenerator;
import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A {@link io.opentelemetry.api.trace.Span}, which acts as a placeholder.
 * This span can be activated via {@link io.opentelemetry.context.Context#with(ImplicitContextKeyed)}} normally and act as a parent of other spans.
 * <p>
 * However, when {@link #end()}/{@link #end(long, TimeUnit)} is called, the span is not exported immediately.
 * Instead, it will only be exported after both {@link #end()}/{@link #end(long, TimeUnit)} and {@link #exportWithParent(Span, Object)} have been called.
 */
public class PlaceholderSpan implements Span {

    /**
     * Stores the attributes added to this span.
     */
    private Attributes attributes = Attributes.empty();

    private String spanName;

    private SpanKind spanKind;

    private Supplier<Long> clock;

    /**
     * Start time in nanoTime, see {@link Clock#nanoTime()}
     */
    private long startNanoTime;

    /**
     * End time in nanoTime, see {@link Clock#nanoTime()}
     */
    private long endNanoTime = 0L;

    private Span newParent;

    @Getter
    private SpanContext spanContext;

    private boolean exported = false;

    private TimeUnit timeUnit = TimeUnit.NANOSECONDS;

    private Object anchoredClock;

    PlaceholderSpan(SpanContext defaultParent, String spanName, SpanKind kind, Supplier<Long> clock) {
        spanContext = generateContext(defaultParent);
        //, EnumSet.of(Span.Options.RECORD_EVENTS));
        this.spanName = spanName;
        spanKind = kind;
        this.clock = clock;
        startNanoTime = clock.get();
    }

    private static SpanContext generateContext(SpanContext parentContext) {
        String id = IdGenerator.random().generateSpanId();
        return SpanContext.create(parentContext.getTraceId(), id, parentContext.getTraceFlags(), parentContext.getTraceState());
    }

    @Override
    public Span setAttribute(AttributeKey key, Object value) {
        attributes.toBuilder().put(key, value);
        return this;
    }

    /**
     * Alters the parent of this span. May only be called exactly once.
     *
     * @param newParent     the parent to use
     * @param anchoredClock the timestamp converter to use
     */
    public synchronized void exportWithParent(Span newParent, Object anchoredClock) {
        this.anchoredClock = anchoredClock;
        this.newParent = newParent;
        if (endNanoTime != 0) {
            export();
        }
    }

    private void export() {
        if (!exported) {
            exported = true;
            Span span = CustomSpanBuilder.builder(spanName, newParent)
                    .kind(spanKind)
                    .customTiming(startNanoTime, endNanoTime, anchoredClock)
                    .spanId(getSpanContext().getSpanId())
                    .attributes(attributes)
                    .startSpan();
            span.end();
        }
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public String getSpanName() {
        return spanName;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        // not yet implemented
        return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        // not yet implemented
        return this;
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
        // not yet implemented
        return this;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        // not yet implemented
        return this;
    }

    @Override
    public Span updateName(String name) {
        spanName = name;
        return this;
    }

    @Override
    public void end() {
        endNanoTime = clock.get();
        if (newParent != null) {
            export();
        }
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
        endNanoTime = timestamp;
        timeUnit = unit;
        if (newParent != null) {
            export();
        }
    }

    @Override
    public boolean isRecording() {
        return false;
    }
}

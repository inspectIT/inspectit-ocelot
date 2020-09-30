package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.implcore.internal.TimestampConverter;
import io.opencensus.trace.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A {@link Span}, which acts as a placeholder.
 * This span can be actiavated via {@link Tracer#withSpan(Span)} normally and act as a parent of other spans.
 * <p>
 * However, when {@link #end(EndSpanOptions)} is called, the span is not exported immidately.
 * Instead it will only be exported after both {@link #end(EndSpanOptions)} and {@link #exportWithParent(Span, TimestampConverter)} have been called.
 */
public class PlaceholderSpan extends Span {

    /**
     * Used for generating a span-ids
     */
    private static final Random RANDOM = new Random();

    /**
     * Stores the attributes added to this span.
     */
    private Map<String, AttributeValue> attributes = new HashMap<>();

    private String spanName;

    private Span.Kind spanKind;

    private Supplier<Long> clock;

    private long startTime;

    private long endTime = 0L;

    private Span newParent;

    private boolean exported = false;

    private TimestampConverter converter;

    PlaceholderSpan(SpanContext defaultParent, String spanName, Span.Kind kind, Supplier<Long> clock) {
        super(generateContext(defaultParent), EnumSet.of(Span.Options.RECORD_EVENTS));
        this.spanName = spanName;
        spanKind = kind;
        this.clock = clock;
        startTime = clock.get();
    }

    private static SpanContext generateContext(SpanContext parentContext) {
        SpanId id = SpanId.generateRandomId(RANDOM);
        return SpanContext.create(parentContext.getTraceId(), id, parentContext.getTraceOptions(), parentContext
                .getTracestate());
    }

    @Override
    public void putAttribute(String key, AttributeValue value) {
        attributes.put(key, value);
    }

    @Override
    public void putAttributes(Map<String, AttributeValue> attributes) {
        this.attributes.putAll(attributes);
    }

    @Override
    public void addAnnotation(String description, Map<String, AttributeValue> attributes) {
        //not supported yet
    }

    @Override
    public void addAnnotation(Annotation annotation) {
        //not supported yet
    }

    @Override
    public void addLink(Link link) {
        //not supported yet
    }

    @Override
    public synchronized void end(EndSpanOptions options) {
        endTime = clock.get();
        if (newParent != null) {
            export();
        }
    }

    /**
     * Alters the parent of this span. May only be called exactly once.
     *
     * @param newParent the parent to use
     * @param converter the timestamp converter to use
     */
    public synchronized void exportWithParent(Span newParent, TimestampConverter converter) {
        this.converter = converter;
        this.newParent = newParent;
        if (endTime != 0) {
            export();
        }
    }

    private void export() {
        if (!exported) {
            exported = true;
            Span span = CustomSpanBuilder.builder(spanName, newParent)
                    .kind(spanKind)
                    .customTiming(startTime, endTime, converter)
                    .spanId(getContext().getSpanId())
                    .startSpan();
            span.putAttributes(attributes);
            span.end();
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public String getSpanName() {
        return spanName;
    }
}

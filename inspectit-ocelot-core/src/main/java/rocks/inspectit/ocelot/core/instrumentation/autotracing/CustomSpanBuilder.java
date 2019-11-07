package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.common.Clock;
import io.opencensus.common.Timestamp;
import io.opencensus.implcore.internal.TimestampConverter;
import io.opencensus.implcore.trace.RecordEventsSpanImpl;
import io.opencensus.implcore.trace.TracerImpl;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.SpanId;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceParams;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * Allows building of spans with custom timestamps.
 */
public class CustomSpanBuilder {

    /**
     * The options member of {@link TracerImpl}.
     */
    private static final Field TRACERIMPL_OPTIONS;

    /**
     * The startEndHandler member of {@link io.opencensus.implcore.trace.SpanBuilderImpl.Options}.
     */
    private static final Field SPANBUILDEROPTIONS_STARTENDHANDLER;

    /**
     * The startEndHandler member of {@link io.opencensus.implcore.trace.SpanBuilderImpl.Options}.
     */
    private static final Field RECORDEVENTSSPANIMPL_TIMESTAMPCONVERTER;

    /**
     * Random used to generate span IDs.
     */
    private static final Random RANDOM = new Random();

    /**
     * The span to use as parent.
     */
    private Span parent;

    /**
     * The name to use for the span.
     */
    private String name;

    /**
     * The spanId to use, null if it should be autogenerated.
     */
    private SpanId spanId;

    /**
     * The timestamp converter to use for the overriden timestamp {@link #entryNanos} and {@link #exitNanos}.
     */
    private TimestampConverter converter;

    /**
     * The span kind to assign to the span.
     */
    private Span.Kind kind;

    /**
     * The timestamps to use for the new span. If not set, the span takes times as usual.
     */
    private long entryNanos = 0;

    private long exitNanos = 0;

    static {
        try {
            TRACERIMPL_OPTIONS = TracerImpl.class.getDeclaredField("spanBuilderOptions");
            TRACERIMPL_OPTIONS.setAccessible(true);
            Class<?> spanBuilderOptionsClass = TRACERIMPL_OPTIONS.get(Tracing.getTracer()).getClass();
            SPANBUILDEROPTIONS_STARTENDHANDLER = spanBuilderOptionsClass.getDeclaredField("startEndHandler");
            SPANBUILDEROPTIONS_STARTENDHANDLER.setAccessible(true);

            RECORDEVENTSSPANIMPL_TIMESTAMPCONVERTER = RecordEventsSpanImpl.class.getDeclaredField("timestampConverter");
            RECORDEVENTSSPANIMPL_TIMESTAMPCONVERTER.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CustomSpanBuilder(String name, Span parent) {
        this.parent = parent;
        this.name = name;
    }

    /**
     * Utility method for extracting a timestamp-converter from a given span.
     *
     * @param span the span to extract the converter from.
     *
     * @return the extracted converter.
     */
    public static TimestampConverter getTimestampConverter(RecordEventsSpanImpl span) {
        try {
            return (TimestampConverter) RECORDEVENTSSPANIMPL_TIMESTAMPCONVERTER.get(span);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not extract timestamp converter", e);
        }
    }

    public static CustomSpanBuilder builder(String name, Span parent) {
        return new CustomSpanBuilder(name, parent);
    }

    /**
     * Replaces the timings on the resulting Span.
     * A timestamp converter must be given if the parent is not a sampled span, otherwise it can be null.
     *
     * @param entryNanos the nano entry timestamp
     * @param exitNanos  the nano exit timestamp
     * @param converter  the timestamp converter to use, if null it will be derived from the parent span.
     *
     * @return the builder
     */
    public CustomSpanBuilder customTiming(long entryNanos, long exitNanos, TimestampConverter converter) {
        if (converter == null) {
            if ((parent instanceof RecordEventsSpanImpl)) {
                this.converter = getTimestampConverter((RecordEventsSpanImpl) parent);
            } else {
                throw new IllegalArgumentException("converter may only be null if the parent is a RecordEventsSpanImpl");
            }
        } else {
            this.converter = converter;
        }
        this.entryNanos = entryNanos;
        this.exitNanos = exitNanos;
        return this;
    }

    public CustomSpanBuilder spanId(SpanId id) {
        spanId = id;
        return this;
    }

    public CustomSpanBuilder kind(Span.Kind kind) {
        this.kind = kind;
        return this;
    }

    public Span startSpan() {
        SpanContext parentContext = parent.getContext();
        TraceParams params = Tracing.getTraceConfig().getActiveTraceParams();
        SpanId id = spanId != null ? spanId : SpanId.generateRandomId(RANDOM);

        SpanContext context = SpanContext.create(parentContext.getTraceId(), id, parentContext.getTraceOptions(), parentContext
                .getTracestate());

        if (entryNanos != 0) {
            DummyClock clock = new DummyClock();
            clock.setValue(entryNanos);
            RecordEventsSpanImpl result = RecordEventsSpanImpl.startSpan(context, name, kind, parentContext.getSpanId(), false, params, getStartEndHandler(), converter, clock);
            clock.setValue(exitNanos);
            return result;
        } else {
            Clock clock = Tracing.getClock();
            return RecordEventsSpanImpl.startSpan(context, name, kind, parentContext.getSpanId(), false, params, getStartEndHandler(), converter, clock);
        }
    }

    private static RecordEventsSpanImpl.StartEndHandler getStartEndHandler() {
        try {
            Object options = TRACERIMPL_OPTIONS.get(Tracing.getTracer());
            return (RecordEventsSpanImpl.StartEndHandler) SPANBUILDEROPTIONS_STARTENDHANDLER.get(options);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not extract start/end handler", e);
        }
    }

    private static class DummyClock extends Clock {

        @Setter
        private long value;

        @Override
        public Timestamp now() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long nowNanos() {
            return value;
        }
    }
}

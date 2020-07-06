package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import io.opencensus.trace.*;
import io.opencensus.trace.propagation.SpanContextParseException;
import io.opencensus.trace.propagation.TextFormat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The extractor and injector implementation for using the Datadog header format.
 */
public class DatadogFormat extends TextFormat {

    /**
     * Singleton instance of this class.
     */
    public static final DatadogFormat INSTANCE = new DatadogFormat();

    private static final Tracestate TRACESTATE_DEFAULT = Tracestate.builder().build();

    /**
     * Datadog header keys.
     */
    private static final String X_DATADOG_TRACE_ID = "X-Datadog-Trace-ID";
    private static final String X_DATADOG_PARENT_ID = "X-Datadog-Parent-ID";
    private static final String X_DATADOG_SAMPLING_PRIORITY = "X-Datadog-Sampling-Priority";

    /**
     * Used as the upper TraceId.SIZE hex characters of the traceID. Datadog used to send TraceId.SIZE hex characters (8-bytes traceId).
     */
    private static final String UPPER_TRACE_ID = "0000000000000000";

    /**
     * Fields used by this format.
     */
    private static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(X_DATADOG_TRACE_ID, X_DATADOG_PARENT_ID));

    /**
     * Hidden constructor.
     */
    private DatadogFormat() {
    }

    @Override
    public List<String> fields() {
        return FIELDS;
    }

    @Override
    public <C> void inject(SpanContext spanContext, C carrier, Setter<C> setter) {
        checkNotNull(spanContext, "spanContext");
        checkNotNull(setter, "setter");
        setter.put(carrier, X_DATADOG_TRACE_ID, String.valueOf(spanContext.getTraceId().getLowerLong()));
        setter.put(carrier, X_DATADOG_PARENT_ID, String.valueOf(convertSpanId(spanContext.getSpanId())));
    }

    @Override
    public <C> SpanContext extract(C carrier, Getter<C> getter) throws SpanContextParseException {
        checkNotNull(carrier, "carrier");
        checkNotNull(getter, "getter");
        try {
            String traceIdStr = getter.get(carrier, X_DATADOG_TRACE_ID);
            if (traceIdStr == null) {
                throw new SpanContextParseException("Missing X_DATADOG_TRACE_ID.");
            }

            // This is an 8-byte traceID - the remaining digits are filled with 0.
            // the actual traceID is at the beginning because the traceID internally uses little-endian order
            traceIdStr = Long.toHexString(Long.parseLong(traceIdStr)) + UPPER_TRACE_ID;

            TraceId traceId = TraceId.fromLowerBase16(traceIdStr);

            String spanIdStr = getter.get(carrier, X_DATADOG_PARENT_ID);
            if (spanIdStr == null) {
                throw new SpanContextParseException("Missing X_DATADOG_PARENT_ID.");
            }
            SpanId spanId = SpanId.fromLowerBase16(Long.toHexString(Long.parseLong(spanIdStr)));

            String samplingPriority = getter.get(carrier, X_DATADOG_SAMPLING_PRIORITY);
            TraceOptions traceOptions = TraceOptions.DEFAULT;
            if ("1".equals(samplingPriority)) {
                traceOptions = TraceOptions.builder().setIsSampled(true).build();
            }

            return SpanContext.create(traceId, spanId, traceOptions, TRACESTATE_DEFAULT);
        } catch (IllegalArgumentException e) {
            throw new SpanContextParseException("Invalid input.", e);
        }
    }

    /**
     * This method is based on the io.opencensus.exporter.trace.datadog.DatadogExporterHandler#convertSpanId method.
     *
     * @param spanId the span id to convert to a long
     * @return the long representing the given span id
     */
    private static long convertSpanId(final SpanId spanId) {
        final byte[] bytes = spanId.getBytes();
        long result = 0;
        for (int i = 0; i < Long.SIZE / Byte.SIZE; i++) {
            result <<= Byte.SIZE;
            result |= (bytes[i] & 0xff);
        }
        if (result < 0) {
            return -result;
        }
        return result;
    }
}

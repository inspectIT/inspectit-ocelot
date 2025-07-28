package rocks.inspectit.ocelot.core.instrumentation.context.propagation;

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The extractor and injector implementation for using the Datadog header format.
 */
public class DatadogFormat implements TextMapPropagator {

    /**
     * Singleton instance of this class
     */
    public static final DatadogFormat INSTANCE = new DatadogFormat();

    private static final TraceState TRACESTATE_DEFAULT = TraceState.getDefault();

    /**
     * Datadog header keys
     */
    private static final String X_DATADOG_TRACE_ID = "X-Datadog-Trace-ID";

    private static final String X_DATADOG_PARENT_ID = "X-Datadog-Parent-ID";

    private static final String X_DATADOG_SAMPLING_PRIORITY = "X-Datadog-Sampling-Priority";

    /**
     * Used as the upper TraceId.SIZE hex characters of the traceID. Datadog used to send TraceId.SIZE hex characters (8-bytes traceId)
     */
    private static final String UPPER_TRACE_ID = "0000000000000000";

    /**
     * Fields used by this format
     */
    private static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
            X_DATADOG_TRACE_ID, X_DATADOG_PARENT_ID
    ));

    /**
     * Fields used by this format in lowercase
     */
    private static final List<String> FIELDS_LOWER = Collections.unmodifiableList(Arrays.asList(
            X_DATADOG_TRACE_ID.toLowerCase(), X_DATADOG_PARENT_ID.toLowerCase()
    ));

    /**
     * Hidden constructor
     */
    private DatadogFormat() {}

    @Override
    public List<String> fields() {
        return Stream.concat(FIELDS.stream(), FIELDS_LOWER.stream()).collect(Collectors.toList());
    }

    @Override
    public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
        SpanContext spanContext = Span.fromContext(context).getSpanContext();

        checkNotNull(context, "spanContext");
        checkNotNull(setter, "setter");
        setter.set(carrier, X_DATADOG_TRACE_ID, String.valueOf(convertTraceId(spanContext.getTraceId())));
        setter.set(carrier, X_DATADOG_PARENT_ID, String.valueOf(convertSpanId(spanContext.getSpanId())));
    }

    @Override
    public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) throws RuntimeException {
        checkNotNull(carrier, "carrier");
        checkNotNull(getter, "getter");
        try {
            String traceIdStr = getHeader(carrier, getter, X_DATADOG_TRACE_ID);
            if (traceIdStr == null) {
                throw new RuntimeException("Missing X_DATADOG_TRACE_ID");
            }

            // This is an 8-byte traceID - the remaining digits are filled with 0.
            // the actual traceID is at the beginning because the traceID internally uses little-endian order
            traceIdStr = TraceId.fromLongs(Long.parseLong(traceIdStr), Long.parseLong(UPPER_TRACE_ID));

            String spanId = getHeader(carrier, getter, X_DATADOG_PARENT_ID);
            if (spanId == null) {
                throw new RuntimeException("Missing X_DATADOG_PARENT_ID");
            }

            // convert Datadog SpanId
            spanId = SpanId.fromLong(Long.parseLong(spanId));

            String samplingPriority = getHeader(carrier, getter, X_DATADOG_SAMPLING_PRIORITY);
            TraceFlags traceFlags = TraceFlags.getDefault();
            if ("1".equals(samplingPriority)) {
                traceFlags = TraceFlags.getSampled();
            }
            Span span = Span.wrap(SpanContext.create(traceIdStr, spanId, traceFlags, TRACESTATE_DEFAULT));
            Context result = context.with(span);
            return result;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid input:", e);
        }
    }

    /**
     * Tries to get a header with fallback to lowercase.
     */
    private <C> String getHeader(C carrier, TextMapGetter<C> getter, String headerName) {
        String value = getter.get(carrier, headerName);
        if (value != null) return value;

        return getter.get(carrier, headerName.toLowerCase());
    }

    /**
     * Gets the long representing the given {@code spanId} in Datadog format
     *
     * @param spanId
     *
     * @return the long representing the given {@code spanId} in Datadog format
     */
    private static long convertSpanId(String spanId) {
        return OtelEncodingUtils.longFromBase16String(spanId, 0);
    }

    /**
     * Converts the {@code traceId} into [lower/upper] long
     *
     * @param traceId the trace id to convert to a long
     *
     * @return the long representing the given trace id
     */
    private static long convertTraceId(String traceId) {
        // alternative method from https://docs.datadoghq.com/tracing/other_telemetry/connect_logs_and_traces/opentelemetry/?tab=java
        //String traceLowerHex = traceId.substring(0, 16);
        //Long datadog = Long.parseUnsignedLong(traceLowerHex, 16);

        // internal use of OTEL
        return OtelEncodingUtils.longFromBase16String(traceId, 0);

    }
}

package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;
import rocks.inspectit.ocelot.core.utils.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Utility class for creating {@link SpanData} instances from {@link Span} ones. This class is in the OpenTelemetry package
 * because it requires access to package-local classes, e.g. {@link SdkSpan} or {@link AnchoredClock}.
 */
@Slf4j
public class OcelotSpanUtils {

    /**
     * The {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl#otelSpan otelSpan} member of {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl}
     */
    private static final Field OPENTELEMETRYSPANIMPL_OTELSPAN;

    /**
     * The {@link io.opentelemetry.sdk.trace.AnchoredClock clock} member of {@link io.opentelemetry.sdk.trace.SdkSpan}
     */
    private static final Field SDKSPAN_CLOCK;

    /**
     * The class of {@link io.opentelemetry.sdk.trace.SdkSpan}.
     */
    private static final Class<? extends Span> SDKSPAN_CLASS;

    /**
     * The class of {@link AnchoredClock}
     */
    private final static Class<?> ANCHOREDCLOCK_CLASS;

    /**
     * The {@link io.opentelemetry.api.internal.AutoValue_ImmutableSpanContext#spanId spanId} member of the {@link io.opentelemetry.api.internal.AutoValue_ImmutableSpanContext spanContext}
     */

    private static Field SPANCONTEXT_SPANID;

    static {
        try {

            OPENTELEMETRYSPANIMPL_OTELSPAN = ReflectionUtils.getFieldAndMakeAccessible("io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl", "otelSpan");

            SDKSPAN_CLASS = (Class<Span>) Class.forName("io.opentelemetry.sdk.trace.SdkSpan");
            SDKSPAN_CLOCK = ReflectionUtils.getFieldAndMakeAccessible(SDKSPAN_CLASS, "clock");

            ANCHOREDCLOCK_CLASS = Class.forName("io.opentelemetry.sdk.trace.AnchoredClock");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@link AnchoredClock} for the given {@link SdkSpan}
     *
     * @param span
     *
     * @return
     */
    public static Object getAnchoredClock(Span span) {
        if (!SDKSPAN_CLASS.isInstance(span)) {
            throw new IllegalArgumentException(span.getClass() + " is no of type " + SDKSPAN_CLASS.getDeclaringClass());
        }
        try {
            return SDKSPAN_CLOCK.get(span);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the {@link AnchoredClock} for the given {@link io.opencensus.trace.Span span}, which is assumed to be an instance of {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl}
     *
     * @param ocSpan
     *
     * @return
     */
    public static Object getAnchoredClock(io.opencensus.trace.Span ocSpan) {
        return getAnchoredClock(OpenCensusShimUtils.getOtelSpan(ocSpan));
    }

    /**
     * Returns the {@link AnchoredClock#startTime()} for the given {@link SdkSpan}
     *
     * @param span
     *
     * @return
     */
    public static long getAnchoredClockStartTime(Span span) {
        return AnchoredClockUtils.getStartTime(getAnchoredClock(span));
    }

    /**
     * Sets the {@link AnchoredClock#clock} of {@link SdkSpan#clock}
     *
     * @param span
     * @param clock
     */
    public static void setAnchoredClockClock(Span span, Clock clock) {
        AnchoredClockUtils.setClock(getAnchoredClock(span), clock);
    }

    /**
     * Sets a new {@link SdkSpan#clock clock} of the {@link io.opentelemetry.sdk.trace.SdkSpan span}
     *
     * @param span
     * @param anchoredClock
     *
     * @return
     */
    public static void setAnchoredClock(Span span, Object anchoredClock) {
        if (!SDKSPAN_CLASS.isInstance(span)) {
            throw new IllegalArgumentException(span.getClass() + " is not of type SdkSpan (" + SDKSPAN_CLASS.getDeclaringClass() + ")");
        }
        if (!ANCHOREDCLOCK_CLASS.isInstance(anchoredClock)) {
            throw new IllegalArgumentException(anchoredClock.getClass() + " is not of type AnchoredClock (" + ANCHOREDCLOCK_CLASS.getDeclaringClass() + ")");
        }
        try {
            SDKSPAN_CLOCK.set(span, anchoredClock);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not set AnchoredClock", e);
        }
    }

    /**
     * Sets the span id of the {@link Span#getSpanContext() span context} ({@link io.opentelemetry.api.internal.AutoValue_ImmutableSpanContext#spanId}) for the given {@link Span}
     *
     * @param span
     * @param spanId
     */
    public static void setSpanId(Span span, String spanId) {
        try {
            // get the spanId field in case this has not been set yet.
            // we get the field in runtime as the underlying class (currently AutoValue_ImmutableSpanContext) may change
            if (null == SPANCONTEXT_SPANID) {
                SPANCONTEXT_SPANID = ReflectionUtils.getFieldAndMakeAccessible(span.getSpanContext()
                        .getClass(), "spanId");
            }
            SPANCONTEXT_SPANID.set(span.getSpanContext(), spanId);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set spanId for " + span.getSpanContext(), e);
        }
    }

}

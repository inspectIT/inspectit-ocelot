package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;
import rocks.inspectit.ocelot.core.utils.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * Utility class for creating {@link SpanData} instances from {@link Span} ones. This class is in the OpenTelemetry package
 * because it requires access to package-local classes, e.g. {@link SdkSpan} or {@link AnchoredClock}.
 * <p>
 * Currently, we are accessing everything via reflection, as we encounter 'Illegal access' exceptions if OpenTelemetry is published to the bootstrap classloader when we use the 'real' classes and members.
 */
@Slf4j
public class OcelotSpanUtils {

    /**
     * The class of {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl}
     */
    private static final Class<Span> OPENTELEMETRYSPANIMPL_CLASS;

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
            OPENTELEMETRYSPANIMPL_CLASS = (Class<Span>) Class.forName("io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl");

            SDKSPAN_CLASS = (Class<Span>) Class.forName("io.opentelemetry.sdk.trace.SdkSpan");
            SDKSPAN_CLOCK = ReflectionUtils.getFieldAndMakeAccessible(SDKSPAN_CLASS, "clock");

            ANCHOREDCLOCK_CLASS = Class.forName("io.opentelemetry.sdk.trace.AnchoredClock");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@link AnchoredClock} for the given {@link SdkSpan} or {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl#otelSpan}
     *
     * @param span
     *
     * @return
     */
    public static Object getAnchoredClock(Span span) {
        if (!SDKSPAN_CLASS.isInstance(span) && !OPENTELEMETRYSPANIMPL_CLASS.isInstance(span)) {
            throw new IllegalArgumentException(span.getClass() + " is not of type " + SDKSPAN_CLASS + " or " + OPENTELEMETRYSPANIMPL_CLASS);
        }
        try {
            return SDKSPAN_CLOCK.get(SDKSPAN_CLASS.isInstance(span) ? span : OpenCensusShimUtils.getOtelSpan((io.opencensus.trace.Span) span));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the {@link SdkSpan#clock} for the given {@link SdkSpan span} with the given {@link AnchoredClock}
     *
     * @param span
     * @param anchoredClock
     *
     * @return
     */
    public static void setAnchoredClock(Span span, Object anchoredClock) {
        if (!SDKSPAN_CLASS.isInstance(span)) {
            throw new IllegalArgumentException(span.getClass() + " is not of type SdkSpan (" + SDKSPAN_CLASS + ")");
        }
        if (!ANCHOREDCLOCK_CLASS.isInstance(anchoredClock)) {
            throw new IllegalArgumentException(anchoredClock.getClass() + " is not of type AnchoredClock (" + ANCHOREDCLOCK_CLASS + ")");
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

    public static Span startSpan(SpanContext context, String name, SpanKind kind, Span parent, Clock clock, Attributes attributes, long startEpochNanos) {
        TracerSharedState tracerSharedState = null;
        return SdkSpan.startSpan(context, name, null, kind, parent, Context.current()
                .with(parent), tracerSharedState.getSpanLimits(), tracerSharedState.getActiveSpanProcessor(), clock, tracerSharedState.getResource(), null, null, 0, startEpochNanos);
    }

}

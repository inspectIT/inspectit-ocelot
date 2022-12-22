package rocks.inspectit.ocelot.core.utils;

import io.opencensus.trace.BlankSpan;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@Slf4j
public class OpenCensusShimUtils {

    private static final Field OPENTELEMETRYSPANIMPL_OTELSPAN;

    private static final Class<Span> OPENTELEMETRYSPANIMPL_CLASS;

    private static final Constructor<Span> OPENTELEMETRYSPANIMPL_CONSTRUCTOR;

    static {
        try {
            OPENTELEMETRYSPANIMPL_CLASS = (Class<Span>) Class.forName("io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl");

            OPENTELEMETRYSPANIMPL_OTELSPAN = ReflectionUtils.getFieldAndMakeAccessible(OPENTELEMETRYSPANIMPL_CLASS, "otelSpan");

            OPENTELEMETRYSPANIMPL_CONSTRUCTOR = OPENTELEMETRYSPANIMPL_CLASS.getDeclaredConstructor(io.opentelemetry.api.trace.Span.class);
            OPENTELEMETRYSPANIMPL_CONSTRUCTOR.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl#OTEL_TRACER} to the current {@link GlobalOpenTelemetry#getTracer("io.opentelemetry.opencensusshim")} via reflection.
     * This method should only be called if  {@link GlobalOpenTelemetry#get()} has changed and the {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl#OTEL_TRACER} still references the deprecated {@link Tracer}
     *
     * @return Whether the OTEL_TRACER was successfully updated
     *
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static boolean updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl() {
        return setOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl(GlobalOpenTelemetry.getTracer("io.opentelemetry.opencensusshim"));
    }

    /**
     * Sets the {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl#OTEL_TRACER} to the given {@link Tracer} via reflection.
     * This method should only be called if {@link GlobalOpenTelemetry#get()} has changed and the {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl#OTEL_TRACER} still references the deprecated {@link Tracer}
     *
     * @param tracer The new {@link Tracer}
     *
     * @return Whether the OTEL_TRACER was successfully updated
     *
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static boolean setOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl(Tracer tracer) {

        try {
            Field tracerField = ReflectionUtils.getFieldAndMakeAccessible("io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl", "OTEL_TRACER");
            // set static final field
            ReflectionUtils.setFinalStatic(tracerField, tracer);

            return tracer == tracerField.get(null);
        } catch (Exception e) {
            log.error("Failed to set OTEL_TRACER in OpenTelemetrySpanBuilderImpl", e);
            return false;
        }
    }

    public static Tracer getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl() {
        try {
            Field tracerField = null;
            // make the field accessible
            tracerField = ReflectionUtils.getFieldAndMakeAccessible(Class.forName("io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl"), "OTEL_TRACER");
            return (Tracer) tracerField.get(null);
        } catch (Exception e) {
            log.error("Failed to get OTEL_TRACER of OpenTelemetrySpanBuilderImpl");
            return null;
        }
    }

    // TODO: über Reflection auf die Copy-Helper-Methode zuzugreifen, anstatt diese zu kopieren

    /**
     * Converts an {@link Span OTEL span} to an {@link io.opencensus.trace.Span OC span}
     *
     * @param otSpan
     *
     * @return
     */
    public static io.opencensus.trace.Span convertSpan(Span otSpan) {
        if (otSpan == null) {
            return BlankSpan.INSTANCE;
        }
        try {
            return (io.opencensus.trace.Span) OPENTELEMETRYSPANIMPL_CONSTRUCTOR.newInstance(otSpan);
        } catch (Exception e) {
            throw new RuntimeException("Could not convert span to OpenTelemetrySpanImpl", e);
        }
    }

    /**
     * Maps {@link io.opencensus.trace.Span.Kind oc Span.Kind} to {@link SpanKind otel SpanKind}
     * Copied from {@link SpanConverter}.
     *
     * @param ocKind
     *
     * @return
     */
    public static SpanKind mapKind(io.opencensus.trace.Span.Kind ocKind) {
        if (ocKind == null) {
            return SpanKind.INTERNAL;
        }
        switch (ocKind) {
            case CLIENT:
                return SpanKind.CLIENT;
            case SERVER:
                return SpanKind.SERVER;
        }
        return SpanKind.INTERNAL;
    }

    /**
     * Casts the given {@link io.opencensus.trace.Span} to {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl}
     *
     * @param ocSpan
     *
     * @return
     */
    public static Span castToOpenTelemetrySpanImpl(io.opencensus.trace.Span ocSpan) {
        if (!OPENTELEMETRYSPANIMPL_CLASS.isInstance(ocSpan)) {
            throw new RuntimeException(String.format("Span '%s' is not instanceof %s", ocSpan.getClass()
                    .getName(), OPENTELEMETRYSPANIMPL_CLASS.getDeclaringClass().getName()));
        }
        return OPENTELEMETRYSPANIMPL_CLASS.cast(ocSpan);
    }

    /**
     * Gets the {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl#otelSpan} for the given {@link io.opentelemetry.opencensusshim.OpenTelemetrySpanImpl}
     *
     * @param ocSpan
     *
     * @return
     */
    public static Span getOtelSpan(io.opencensus.trace.Span ocSpan) {
        Span otelSpan = castToOpenTelemetrySpanImpl(ocSpan);
        try {
            return (Span) OPENTELEMETRYSPANIMPL_OTELSPAN.get(otelSpan);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

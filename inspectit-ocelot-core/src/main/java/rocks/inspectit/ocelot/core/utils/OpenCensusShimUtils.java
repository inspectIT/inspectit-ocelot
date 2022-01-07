package rocks.inspectit.ocelot.core.utils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public class OpenCensusShimUtils {

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
        Field tracerField = null;
        try {
            tracerField = Class.forName("io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl")
                    .getDeclaredField("OTEL_TRACER");

            // set static final field
            ReflectionUtils.setFinalStatic(tracerField, tracer);

            return true;
        } catch (Exception e) {
            log.error("Failed to set OTEL_TRACER in OpenTelemetrySpanBuilderImpl", e);
            return false;
        }
    }

    public static Tracer getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl() {
        Field tracerField = null;
        try {
            // make the field accessible
            tracerField = ReflectionUtils.getFinalStaticFieldAndMakeAccessible(Class.forName("io.opentelemetry.opencensusshim.OpenTelemetrySpanBuilderImpl"), "OTEL_TRACER", true);
            Tracer tracer = (Tracer) tracerField.get(null);
            return tracer;
        } catch (Exception e) {
            log.error("Failed to get OTEL_TRACER of OpenTelemetrySpanBuilderImpl");
            return null;
        }

    }

}

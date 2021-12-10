package rocks.inspectit.ocelot.core.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenCensusShimUtilsTest extends SpringTestBase {

    @Test
    void testUpdateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl() throws InterruptedException {
        Tracer tracer = OpenCensusShimUtils.getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl();

        // reset OTEL
        GlobalOpenTelemetry.resetForTest();

        // build and register new OTEL
        OpenTelemetrySdk.builder().buildAndRegisterGlobal();

        // update the OTEL_TRACER
        OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();

        Tracer newTracer = OpenCensusShimUtils.getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl();

        // assert that the OTEL_TRACER has changed
        assertThat(tracer).isNotSameAs(newTracer);
    }

    @Test
    void testSetOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl() {
        // get current OTEL_TRACER
        Tracer tracer = OpenCensusShimUtils.getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl();

        // set to a different tracer
        OpenCensusShimUtils.setOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl(GlobalOpenTelemetry.getTracer("this.is.my.tracer"));

        // get new OTEL_TRACER
        Tracer newTracer = OpenCensusShimUtils.getOpenTelemetryTracerOfOpenTelemetrySpanBuilderImpl();

        // assert that the OTEL_TRACER has changed
        assertThat(tracer).isNotSameAs(newTracer);
    }
}

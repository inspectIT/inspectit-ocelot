package rocks.inspectit.ocelot.core.utils;

import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.SpringTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenCensusShimUtilsTest extends SpringTestBase {

    @Test
    void testUpdateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl() throws ClassNotFoundException, IllegalAccessException {

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

    @Test
    void testConvertSpan() {
        Span otSpan = OpenTelemetryUtils.getTracer().spanBuilder("my-test-span").startSpan();
        otSpan.end();
        io.opencensus.trace.Span ocSpan = OpenCensusShimUtils.convertSpan(otSpan);

        assertThat(ocSpan != null).isTrue();
        assertThat(ocSpan.getContext().getSpanId().isValid()).isTrue();
        assertThat(ocSpan.getContext().getSpanId().toLowerBase16()).isEqualTo(otSpan.getSpanContext().getSpanId());
    }

    @Test
    void testCastToOpenTelemetrySpanImpl() {
        io.opencensus.trace.Span ocSpan = Tracing.getTracer().spanBuilder("my-test-span").startSpan();
        ocSpan.end();
        Span otSpan = OpenCensusShimUtils.castToOpenTelemetrySpanImpl(ocSpan);
        assertThat(otSpan.getSpanContext().isValid()).isTrue();
        assertThat(otSpan.getClass().getSimpleName()).isEqualTo("OpenTelemetrySpanImpl");
    }

    @Test
    void testGetOtelSpan() {
        io.opencensus.trace.Span ocSpan = Tracing.getTracer().spanBuilder("my-test-span").startSpan();
        ocSpan.end();

        Span otSpan = OpenCensusShimUtils.getOtelSpan(ocSpan);
        assertThat(otSpan.getSpanContext().isValid()).isTrue();
        assertThat(otSpan.getClass().getSimpleName()).isEqualTo("SdkSpan");
    }
}

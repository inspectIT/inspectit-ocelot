package rocks.inspectit.ocelot.core.opentelemetry.trace.samplers;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link HybridParentTraceIdRatioBasedSampler}
 */
public class HybridParentTraceIdRatioBasedSamplerTest {

    private final static HybridParentTraceIdRatioBasedSampler zero = HybridParentTraceIdRatioBasedSampler.create(0.0);

    private final static HybridParentTraceIdRatioBasedSampler one = HybridParentTraceIdRatioBasedSampler.create(1.0);

    private Context getMockContext(boolean isSampled) {
        Context context = mock(Context.class);
        Span span = mock(Span.class);
        SpanContext spanContext = mock(SpanContext.class);
        when(context.get(any())).thenReturn(span);
        when(span.getSpanContext()).thenReturn(spanContext);
        when(spanContext.isValid()).thenReturn(true);
        when(spanContext.isRemote()).thenReturn(false);
        when(spanContext.isSampled()).thenReturn(isSampled);
        return context;
    }

    @Test
    void testParentSampled() {
        assertThat(zero.shouldSample(getMockContext(true), TraceId.getInvalid(), "child", SpanKind.CLIENT, null, null)).isEqualTo(SamplingResult.recordAndSample());
        assertThat(one.shouldSample(getMockContext(true), TraceId.getInvalid(), "child", SpanKind.CLIENT, null, null)).isEqualTo(SamplingResult.recordAndSample());
    }

    @Test
    void testParentNotSampled() {
        assertThat(zero.shouldSample(getMockContext(false), TraceId.getInvalid(), "child", SpanKind.CLIENT, null, null)).isEqualTo(SamplingResult.drop());
        assertThat(one.shouldSample(getMockContext(false), TraceId.getInvalid(), "child", SpanKind.CLIENT, null, null)).isEqualTo(SamplingResult.recordAndSample());
    }
}

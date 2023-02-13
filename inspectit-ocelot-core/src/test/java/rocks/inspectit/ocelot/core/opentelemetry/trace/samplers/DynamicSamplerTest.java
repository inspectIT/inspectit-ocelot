package rocks.inspectit.ocelot.core.opentelemetry.trace.samplers;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DynamicSamplerTest {

    private static final Random RANDOM = new Random();

    private static final int TEST_RUNS = 1000;

    private double testRatio(Sampler sampler, Context parentContext, SamplingDecision expectedValue) {
        return testRatio(sampler, parentContext, expectedValue, TEST_RUNS);
    }

    private double testRatio(Sampler sampler, Context parentContext, SamplingDecision expectedValue, int runs) {
        double successCount = IntStream.range(0, runs)
                .mapToObj(i -> sampler.shouldSample(parentContext, nextTraceId(), null, null, null, null))
                .filter(result -> result.getDecision() == expectedValue)
                .count();
        return successCount / runs;
    }

    private String nextTraceId() {
        return TraceId.fromLongs(RANDOM.nextLong(), RANDOM.nextLong());
    }

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

    @Nested
    public class ShouldSample {

        @Test
        public void noParent_alwaysSample() {
            DynamicSampler sampler = new DynamicSampler(1);

            double ratio = testRatio(sampler, null, SamplingDecision.RECORD_AND_SAMPLE);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void noParent_neverSample() {
            DynamicSampler sampler = new DynamicSampler(0);

            double ratio = testRatio(sampler, null, SamplingDecision.DROP);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void noParent_ratioSample() {
            DynamicSampler sampler = new DynamicSampler(0.5D);

            double ratio = testRatio(sampler, null, SamplingDecision.RECORD_AND_SAMPLE, 10_000);

            assertThat(ratio).isBetween(0.45D, 0.55D);
        }

        @Test
        public void parentSampled_alwaysSample() {
            DynamicSampler sampler = new DynamicSampler(1);

            double ratio = testRatio(sampler, getMockContext(true), SamplingDecision.RECORD_AND_SAMPLE);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void parentSampled_neverSample() {
            DynamicSampler sampler = new DynamicSampler(0);

            double ratio = testRatio(sampler, getMockContext(true), SamplingDecision.RECORD_AND_SAMPLE);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void parentSampled_ratioSample() {
            DynamicSampler sampler = new DynamicSampler(0.5D);

            double ratio = testRatio(sampler, getMockContext(true), SamplingDecision.RECORD_AND_SAMPLE);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void parentNotSampled_alwaysSample() {
            DynamicSampler sampler = new DynamicSampler(1);

            double ratio = testRatio(sampler, getMockContext(false), SamplingDecision.DROP);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void parentNotSampled_neverSample() {
            DynamicSampler sampler = new DynamicSampler(0);

            double ratio = testRatio(sampler, getMockContext(false), SamplingDecision.DROP);

            assertThat(ratio).isEqualTo(1.0D);
        }

        @Test
        public void parentNotSampled_ratioSample() {
            DynamicSampler sampler = new DynamicSampler(0.5D);

            double ratio = testRatio(sampler, getMockContext(false), SamplingDecision.DROP);

            assertThat(ratio).isEqualTo(1.0D);
        }

    }

}
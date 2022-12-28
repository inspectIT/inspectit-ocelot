package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.OcelotAnchoredClockUtils;
import io.opentelemetry.sdk.trace.OcelotSpanUtils;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.core.SpringTestBase;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomSpanBuilderTest {

    @Nested
    class Timestamps extends SpringTestBase {

        @Test
        void verifyTimingsChanged() {
            Span parent = OpenTelemetryUtils.getTracer(Sampler.traceIdRatioBased(1.0)).spanBuilder("root").startSpan();

            long entryNanos = Clock.getDefault().nanoTime() + 42;
            long exitNanos = entryNanos + 100;
            Span mySpan = CustomSpanBuilder.builder("foo", parent)
                    .customTiming(entryNanos, exitNanos, null)
                    .startSpan();
            Object anchoredClock = OcelotSpanUtils.getAnchoredClock(mySpan);

            ReadWriteSpan spanImpl = (ReadWriteSpan) mySpan;
            spanImpl.end();
            assertThat(spanImpl.getName()).isEqualTo("foo");
            assertThat(spanImpl.getSpanContext().getSpanId()).isNotEqualTo(parent.getSpanContext().getSpanId());
            assertThat(spanImpl.getSpanContext().getTraceId()).isEqualTo(parent.getSpanContext().getTraceId());
            assertThat(spanImpl.getSpanContext().getTraceFlags()).isEqualTo(parent.getSpanContext().getTraceFlags());
            assertThat(spanImpl.getSpanContext().getTraceState()).isEqualTo(parent.getSpanContext().getTraceState());
            assertThat(spanImpl.toSpanData().getParentSpanId()).isEqualTo(parent.getSpanContext().getSpanId());
            assertThat(spanImpl.toSpanData()
                    .getEndEpochNanos()).isEqualTo(OcelotAnchoredClockUtils.getStartTime(anchoredClock) + (exitNanos - OcelotAnchoredClockUtils.getNanoTime(anchoredClock)));
            assertThat(spanImpl.getLatencyNanos()).isEqualTo(100);
        }
    }

}

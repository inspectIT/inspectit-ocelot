package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.AnchoredClockUtils;
import io.opentelemetry.sdk.trace.OcelotSpanUtils;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
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
            // TODO: enable sampling for CustomSpanBuilder
            Span parent = OpenTelemetryUtils.getTracer().spanBuilder("root")
                    //.setSampler(Samplers.alwaysSample())
                    .startSpan();

            Span mySpan = CustomSpanBuilder.builder("foo", parent).customTiming(42, 142, null).startSpan();
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
                    .getEndEpochNanos()).isEqualTo(AnchoredClockUtils.getStartTime(anchoredClock) + (142 - AnchoredClockUtils.getNanoTime(anchoredClock)));
            assertThat(spanImpl.getLatencyNanos()).isEqualTo(100);
        }
    }

}

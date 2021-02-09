package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import io.opencensus.implcore.trace.RecordEventsSpanImpl;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomSpanBuilderTest {


    @Nested
    class Timestamps {

        @Test
        void verifyTimingsChanged() {

            Span parent = Tracing.getTracer().spanBuilder("root")
                    .setSampler(Samplers.alwaysSample())
                    .startSpan();

            Span mySpan = CustomSpanBuilder.builder("foo", parent)
                    .customTiming(42, 142, null)
                    .startSpan();

            RecordEventsSpanImpl spanImpl = (RecordEventsSpanImpl) mySpan;
            assertThat(spanImpl.getName()).isEqualTo("foo");
            assertThat(spanImpl.getContext().getSpanId()).isNotEqualTo(parent.getContext().getSpanId());
            assertThat(spanImpl.getContext().getTraceId()).isEqualTo(parent.getContext().getTraceId());
            assertThat(spanImpl.getContext().getTraceOptions()).isEqualTo(parent.getContext().getTraceOptions());
            assertThat(spanImpl.getContext().getTracestate()).isEqualTo(parent.getContext().getTracestate());
            assertThat(spanImpl.toSpanData().getParentSpanId()).isEqualTo(parent.getContext().getSpanId());
            assertThat(spanImpl.getEndNanoTime()).isEqualTo(142);
            assertThat(spanImpl.getLatencyNs()).isEqualTo(100);
        }
    }

}

package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.export.SpanData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoTracingTest extends TraceTestBase {

    @BeforeAll
    static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentation(AutoTracingTest.class, true, 30, TimeUnit.SECONDS);
    }

    SpanData getSpanWithName(Collection<? extends SpanData> spans, String name) {
        Optional<? extends SpanData> spanOptional = spans.stream()
                .filter(s -> ((SpanData) s).getName().equals(name))
                .findFirst();
        assertThat(spanOptional).isNotEmpty();
        return spanOptional.get();
    }

    @Test
    void verifyStackTraceSampling() {
        instrumentMe();

        assertTraceExported((spans) -> {

            SpanData root = getSpanWithName(spans, "AutoTracingTest.instrumentMe");
            SpanData activeWait = getSpanWithName(spans, "*AutoTracingTest.activeWait");
            SpanData passiveWait = getSpanWithName(spans, "*Thread.sleep");

            assertThat(activeWait.getParentSpanId()).isEqualTo(root.getContext().getSpanId());
            assertThat(passiveWait.getParentSpanId()).isEqualTo(root.getContext().getSpanId());

            assertThat(activeWait.getEndTimestamp()).isLessThan(passiveWait.getStartTimestamp());
        });
    }

    private void passiveWait(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void activeWait(long duration) {
        long durationNanos = duration * 1000 * 1000;
        long start = System.nanoTime();
        while ((System.nanoTime() - start) < durationNanos) {
        }
    }

    private void nestedWait(long duration) {
        passiveWait(duration);
    }

    private void instrumentMe() {
        activeWait(150);
        nestedWait(100);
    }

}

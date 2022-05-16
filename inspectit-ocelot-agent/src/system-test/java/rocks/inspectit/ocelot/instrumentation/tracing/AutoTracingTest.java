package rocks.inspectit.ocelot.instrumentation.tracing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled // TODO: fix StackTraceSampling and AutoTracing with OTEL
public class AutoTracingTest extends TraceTestBase {

    @BeforeAll
    static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentation(AutoTracingTest.class, true, 30, TimeUnit.SECONDS);
    }

    io.opentelemetry.sdk.trace.data.SpanData getSpanWithName(Collection<? extends io.opentelemetry.sdk.trace.data.SpanData> spans, String name) {
        Optional<? extends io.opentelemetry.sdk.trace.data.SpanData> spanOptional = spans.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst();
        assertThat(spanOptional).isNotEmpty();
        return spanOptional.get();
    }

    @Test
    void verifyStackTraceSampling() {
        instrumentMe();

        assertTraceExported((spans) -> {

            io.opentelemetry.sdk.trace.data.SpanData root = getSpanWithName(spans, "AutoTracingTest.instrumentMe");
            io.opentelemetry.sdk.trace.data.SpanData activeWait = getSpanWithName(spans, "*AutoTracingTest.activeWait");
            io.opentelemetry.sdk.trace.data.SpanData passiveWait = getSpanWithName(spans, "*Thread.sleep");
            assertThat(activeWait.getParentSpanId()).isEqualTo(root.getSpanId());
            assertThat(passiveWait.getParentSpanId()).isEqualTo(root.getSpanId());

            assertThat(activeWait.getEndEpochNanos()).isLessThan(passiveWait.getEndEpochNanos());
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

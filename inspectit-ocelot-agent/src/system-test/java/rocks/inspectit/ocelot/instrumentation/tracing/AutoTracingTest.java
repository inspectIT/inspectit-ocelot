package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoTracingTest extends TraceTestBase {

    @BeforeAll
    static void setup() {
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
            io.opentelemetry.sdk.trace.data.SpanData passiveWait;

            io.opentelemetry.sdk.trace.data.SpanData compareSpan;

            // Thread.sleep method appears as Thread.sleep0 in Java 21
            if(System.getProperty("java.version").startsWith("21")) {
                // In Java 21 the root span AutoTracingTest.instrumentMe is followed by a child-span *AutoTracingTest.instrumentMe
                compareSpan = getSpanWithName(spans, "*AutoTracingTest.instrumentMe");
                passiveWait = getSpanWithName(spans, "*Thread.sleep0");

            } else {
                compareSpan = root;
                passiveWait = getSpanWithName(spans, "*Thread.sleep");
            }

            assertThat(activeWait.getParentSpanId()).isEqualTo(compareSpan.getSpanId());
            assertThat(passiveWait.getParentSpanId()).isEqualTo(compareSpan.getSpanId());

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

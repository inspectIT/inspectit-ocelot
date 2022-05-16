package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TraceTestBase extends InstrumentationSysTestBase {

    final static InMemorySpanExporter spanExporter = TestUtils.initializeOpenTelemetryForSystemTesting();

    protected List<SpanData> getExportedSpans() {
        return spanExporter.getFinishedSpanItems();
    }

    @BeforeEach
    void setupExporter() {
        spanExporter.reset();
    }

    @AfterEach
    void destroyExporter() {
        spanExporter.reset();
    }

    void assertTraceExported(Consumer<? super List<? extends SpanData>> assertions) {

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, List<io.opentelemetry.sdk.trace.data.SpanData>> traces = getExportedSpans().stream()
                    .collect(Collectors.groupingBy(s -> s.getSpanContext().getTraceId(), Collectors.toList()));
            assertThat(traces.values()).anySatisfy(assertions);
            assertThat(traces.values()).filteredOnAssertions(assertions).hasSize(1);
        });
    }

    void assertSpansExported(Consumer<? super Collection<? extends io.opentelemetry.sdk.trace.data.SpanData>> assertions) {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertions.accept(getExportedSpans());
        });
    }

}

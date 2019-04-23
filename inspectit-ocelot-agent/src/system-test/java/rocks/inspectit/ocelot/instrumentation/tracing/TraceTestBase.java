package rocks.inspectit.ocelot.instrumentation.tracing;

import io.opencensus.trace.TraceId;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.export.SpanData;
import io.opencensus.trace.export.SpanExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import rocks.inspectit.ocelot.instrumentation.InstrumentationSysTestBase;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TraceTestBase extends InstrumentationSysTestBase {

    protected Collection<SpanData> exportedSpans = new ConcurrentLinkedQueue<>();

    @BeforeEach
    void setupExporter() {
        exportedSpans.clear();
        Tracing.getExportComponent().getSpanExporter().registerHandler("mock", new SpanExporter.Handler() {
            @Override
            public void export(Collection<SpanData> spanDataList) {
                exportedSpans.addAll(spanDataList);
            }
        });
    }

    @AfterEach
    void destroyExporter() {
        exportedSpans.clear();
        Tracing.getExportComponent().getSpanExporter().unregisterHandler("mock");
    }

    void assertTraceExported(Consumer<? super List<? extends SpanData>> assertions) {

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<TraceId, List<SpanData>> traces = exportedSpans.stream()
                    .collect(Collectors.groupingBy(s -> s.getContext().getTraceId(), Collectors.toList()));
            assertThat(traces.values()).anySatisfy(assertions);
            assertThat(traces.values()).filteredOnAssertions(assertions).hasSize(1);
        });
    }
}

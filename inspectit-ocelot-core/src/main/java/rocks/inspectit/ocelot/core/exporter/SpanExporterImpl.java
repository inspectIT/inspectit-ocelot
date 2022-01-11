package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class SpanExporterImpl implements SpanExporter {

    private Map<String, SpanExporter> spanExporters = new ConcurrentHashMap<>();

    private Object lock = new Object();

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        CompletableResultCode resultCode;
        synchronized (lock) {
            resultCode = execute(spanExporters.values(), (spanExporter) -> spanExporter.export(spans));
        }
        return resultCode;
    }

    @Override
    public CompletableResultCode flush() {
        CompletableResultCode resultCode;
        synchronized (lock) {
            resultCode = execute(spanExporters.values(), (spanExporter) -> spanExporter.flush());
        }
        return resultCode;
    }

    @Override
    public CompletableResultCode shutdown() {
        CompletableResultCode resultCode;
        synchronized (lock) {
            resultCode = execute(spanExporters.values(), (spanExporter) -> spanExporter.shutdown());
        }
        return resultCode;
    }

    private static CompletableResultCode execute(Collection<SpanExporter> spanExporters, Function<SpanExporter, CompletableResultCode> spanExporterRunHandler) {
        List<CompletableResultCode> resultCodes = new ArrayList<>(spanExporters.size());
        for (SpanExporter spanExporter : spanExporters) {
            resultCodes.add(spanExporterRunHandler.apply(spanExporter));
        }
        return CompletableResultCode.ofAll(resultCodes);
    }
}

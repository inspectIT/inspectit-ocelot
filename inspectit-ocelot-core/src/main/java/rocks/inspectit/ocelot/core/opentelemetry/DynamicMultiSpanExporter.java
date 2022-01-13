package rocks.inspectit.ocelot.core.opentelemetry;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * {@link SpanExporter} that forwards all received spans to a list of {@link SpanExporter}, similar to {@link io.opentelemetry.sdk.trace.export.MultiSpanExporter}. In contrast to {@link io.opentelemetry.sdk.trace.export.MultiSpanExporter}, {@link SpanExporter}s can by dynamically registered and unregisted.
 *
 * <p>Can be used to export to multiple backends using the same {@link io.opentelemetry.sdk.trace.SpanProcessor} like {@link io.opentelemetry.sdk.trace.export.SimpleSpanProcessor} or {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor}
 */
@Slf4j
public class DynamicMultiSpanExporter implements SpanExporter {

    /**
     * The {@link SpanExporter}s that all received spans are forwarded to.
     */
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

    /**
     * Executes the given function on a {@link Collection<SpanExporter>}
     *
     * @param spanExporters   The {@link SpanExporter}s
     * @param spanExporterFun The {@link Function<SpanExporter, CompletableResultCode>} to apply on each {@link SpanExporter}
     *
     * @return The {@link CompletableResultCode} of all applications of the function
     */
    private static CompletableResultCode execute(Collection<SpanExporter> spanExporters, Function<SpanExporter, CompletableResultCode> spanExporterFun) {
        List<CompletableResultCode> resultCodes = new ArrayList<>(spanExporters.size());
        for (SpanExporter spanExporter : spanExporters) {
            CompletableResultCode resultCode;
            try {
                resultCode = spanExporterFun.apply(spanExporter);
            } catch (RuntimeException e) {
                log.error("Exception thrown in execute", e);
                resultCodes.add(CompletableResultCode.ofFailure());
                continue;
            }
            resultCodes.add(resultCode);
        }
        return CompletableResultCode.ofAll(resultCodes);
    }

    /**
     * Registers the given {@link SpanExporter} to export {@link SpanData} for sampled spans.
     *
     * @param registerName The name of the span exporter service. Must be unique for each service.
     * @param spanExporter The {@link SpanExporter} that is called for each {@link #export(Collection)}, {@link #flush()}, and {@link #shutdown()}
     *
     * @return Whether a {@link SpanProcessor} was **not** previously registered with the same name. Returns false if a {@link SpanExporter} with the given name was already registered.
     */
    public boolean registerSpanExporter(String registerName, SpanExporter spanExporter) {
        return null == spanExporters.put(registerName, spanExporter);
    }

    /**
     * Unregisters the given {@link SpanExporter}
     *
     * @param registerName The name of the span exporter service.
     *
     * @return Whether a {@link SpanExporter} with the given name was successfully removed. Returns false if no {@link SpanExporter} with the given name was previously registered.
     */
    public boolean unregisterSpanExporter(String registerName) {
        return null != spanExporters.remove(registerName);
    }

    @Override
    public String toString() {
        return "SpanExporterImpl{" + "spanExporters=" + spanExporters + '}';
    }
}

package rocks.inspectit.ocelot.utils;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.opentelemetry.NoopOpenTelemetryController;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class TraceTestUtils {

    static InMemorySpanExporter inMemorySpanExporter;

    /**
     * Initialize {@link io.opentelemetry.api.OpenTelemetry} with a {@link InMemorySpanExporter} so that we can access the exported {@link io.opentelemetry.api.trace.Span Spans}
     *
     * @return The {@link InMemorySpanExporter} that can be used to retrieve exported {@link io.opentelemetry.api.trace.Span Spans}
     */
    public static InMemorySpanExporter initializeSpanExporterForSystemTesting() {
        // if OTel was already initialized with the inMemorySpanExporter, just reset and return it
        if (null != inMemorySpanExporter && NoopOpenTelemetryController.INSTANCE != Instances.openTelemetryController) {
            inMemorySpanExporter.reset();
            return inMemorySpanExporter;
        }

        // wait until OTel is initialized
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> NoopOpenTelemetryController.INSTANCE != Instances.openTelemetryController);
        // create an InMemorySpanExporter and register it with OTEL
        inMemorySpanExporter = InMemorySpanExporter.create();
        Instances.openTelemetryController.registerTraceExporterService(inMemorySpanExporter, "InMemorySpanExporterService");
        return inMemorySpanExporter;
    }
}

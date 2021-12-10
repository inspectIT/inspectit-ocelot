package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.trace.LoggingTraceExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}
 */
@Component
@Slf4j
public class LoggingTraceExporterService extends DynamicallyActivatableService {

    /**
     * The {@link OpenTelemetry}
     */
    private OpenTelemetry openTelemetry;

    /**
     * The {@link SdkTracerProvider}
     */
    private SdkTracerProvider tracerProvider;

    /**
     * The {@link DynamicallyActivatableSampler} for the {@link #tracerProvider}
     */
    private DynamicallyActivatableSampler sampler;

    /**
     * The {@link SpanProcessor} of the {@link #spanExporter
     */
    private SpanProcessor simpleSpanProcessor;

    /**
     * The {@link DynamicallyActivatableSpanExporter< LoggingSpanExporter >} for exporting the spans to the log
     */
    private DynamicallyActivatableSpanExporter<LoggingSpanExporter> spanExporter;

    /**
     * The service name {@link Resource}
     */
    private Resource serviceNameResource;

    public LoggingTraceExporterService() {
        super("exporters.tracing.logging", "tracing.enabled");
    }

    @Override
    protected void init() {
        super.init();

        // create span exporter and span processors
        spanExporter = DynamicallyActivatableSpanExporter.createLoggingSpanExporter();

        // close the tracer provider when the JVM is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (null != tracerProvider) {
                tracerProvider.shutdown();
            }
        }));
    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig configuration) {
        @Valid LoggingTraceExporterSettings logging = configuration.getExporters().getTracing().getLogging();
        return configuration.getTracing().isEnabled() && logging.isEnabled();

    }

    @Override
    protected boolean doEnable(InspectitConfig conf) {
        LoggingTraceExporterSettings logging = conf.getExporters().getTracing().getLogging();
        try {

            // reset GlobalOpenTelemetry
            GlobalOpenTelemetry.resetForTest();

            // create span processors
            // SpanProcessors are also shut down when the corresponding TracerProvider is shut down. Thus, we need to create the SpanProcessors each time
            simpleSpanProcessor = SimpleSpanProcessor.create(spanExporter);

            // create sampler
            sampler = DynamicallyActivatableSampler.createRatio(env.getCurrentConfig()
                    .getTracing()
                    .getSampleProbability());

            // create Resource for the service name
            serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, logging.getServiceName()));

            // set up tracer provider
            tracerProvider = SdkTracerProvider.builder()
                    .addSpanProcessor(simpleSpanProcessor)
                    .setSampler(sampler)
                    .setResource(serviceNameResource)
                    .build();

            // build and register OTel
            openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                    // TODO: do I also need the W3CBaggagePropagator?
                    // W3CBaggagePropagator.getInstance()
                    .buildAndRegisterGlobal();

            // update OC tracer
            OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();

            // enable span exporter
            spanExporter.doEnable();

            log.info("Starting TraceLoggingSpanExporter");

            return true;
        } catch (Exception e) {
            log.error("Failed to start TraceLoggingExporter", e);
            return false;
        }
    }

    @Override
    protected boolean doDisable() {
        try {
            // disable the span exporter
            if (null != spanExporter && null != tracerProvider) {
                spanExporter.doDisable();
                tracerProvider.forceFlush();
                tracerProvider.shutdown();
                tracerProvider = null;
            }
            log.info("Stopping TraceLoggingSpanExporter");
            return true;
        } catch (Exception e) {
            log.error("Failed to stop TraceLoggingExporter", e);
            return false;
        }
    }
}

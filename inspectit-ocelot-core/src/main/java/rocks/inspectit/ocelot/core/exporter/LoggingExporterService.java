package rocks.inspectit.ocelot.core.exporter;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.opencensusshim.metrics.OpenCensusMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.MetricLoggingExporterSettings;
import rocks.inspectit.ocelot.config.model.exporters.trace.TraceLoggingExporterSettings;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;

import javax.validation.Valid;

/**
 * Service for the {@link io.opentelemetry.exporter.logging.LoggingMetricExporter} and {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}
 */
@Component
@Slf4j
public class LoggingExporterService extends DynamicallyActivatableService {

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
     * The {@link DynamicallyActivatableSpanExporter<LoggingSpanExporter>} for exporting the spans to the log
     */
    private DynamicallyActivatableSpanExporter<LoggingSpanExporter> spanExporter;

    /**
     * The {@link SdkMeterProvider}
     */
    private SdkMeterProvider meterProvider;

    /**
     * The {@link DynamicallyActivatableMetricExporter} for exporting metrics to the log
     */
    private DynamicallyActivatableMetricExporter<LoggingMetricExporter> metricExporter;

    /**
     * The {@link PeriodicMetricReader} for reading metrics to the log
     */
    private PeriodicMetricReaderBuilder metricReader;

    /**
     * The service name {@link Resource}
     */
    private Resource serviceNameResource;

    public LoggingExporterService() {
        super("exporters.metrics.logging", "metrics.enabled", "exporters.tracing.logging");
    }

    @Override
    protected void init() {
        super.init();

        // create span exporter and span processors
        spanExporter = DynamicallyActivatableSpanExporter.createLoggingSpanExporter();

        // create new metric exporter
        metricExporter = DynamicallyActivatableMetricExporter.createLoggingExporter();

        // close the tracer provider when the JVM is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (null != tracerProvider) {
                tracerProvider.shutdown();
            }
            if (null != meterProvider) {
                meterProvider.shutdown();
            }
        }));

    }

    @Override
    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        @Valid MetricLoggingExporterSettings metricLogging = conf.getExporters().getMetrics().getLogging();
        @Valid TraceLoggingExporterSettings traceLogging = conf.getExporters().getTracing().getLogging();
        return (traceLogging.isEnabled() || metricLogging.isEnabled()) && conf.getMetrics().isEnabled();
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        MetricLoggingExporterSettings metricLogging = configuration.getExporters().getMetrics().getLogging();
        TraceLoggingExporterSettings traceLogging = configuration.getExporters().getTracing().getLogging();

        try {

            // enable trace logging
            if (traceLogging.isEnabled()) {

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
                serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, traceLogging.getServiceName()));

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

                log.info("Starting LoggingSpanExporter");
            }

            if (metricLogging.isEnabled()) {
                // build and register the MeterProvider if null
                metricReader = PeriodicMetricReader.builder(metricExporter)
                        .setInterval(metricLogging.getExportInterval());

                meterProvider = SdkMeterProvider.builder()
                        .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, configuration.getServiceName())))
                        .registerMetricReader(OpenCensusMetrics.attachTo(metricReader.newMetricReaderFactory()))
                        .buildAndRegisterGlobal();

                // enable the metric exporter
                metricExporter.doEnable();
                log.info("Starting LoggingMetricsExporter");
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to start LoggingExporter", e);
            return false;
        }

    }

    @Override
    protected boolean doDisable() {
        try {
            // disable the span exporter
            if (null != spanExporter && null != tracerProvider && !env.getCurrentConfig()
                    .getExporters()
                    .getTracing()
                    .getLogging()
                    .isEnabled()) {
                spanExporter.doDisable();
                tracerProvider.forceFlush();
                tracerProvider.shutdown();
                tracerProvider = null;
                log.info("Stopping LoggingSpanExporter");
            }

            if (null != metricExporter && null != meterProvider && !env.getCurrentConfig()
                    .getExporters()
                    .getMetrics()
                    .getLogging()
                    .isEnabled()) {
                if (null != meterProvider) {
                    // flush all metrics before disabling them
                    meterProvider.forceFlush();
                    meterProvider.shutdown();
                    meterProvider = null;
                }
                metricExporter.doDisable();
                log.info("Stopping LoggingMetricExporter");
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to stop LoggingExporter", e);
            return false;
        }
    }
}

package rocks.inspectit.ocelot.core.opentelemetry;

import io.opencensus.trace.Tracing;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.opencensusshim.metrics.OpenCensusMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.opentelemetry.IOpenTelemetryController;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableMetricsExporterService;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableSpanExporter;
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableTraceExporterService;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The implementation of {@link IOpenTelemetryController}. The {@link OpenTelemetryControllerImpl} configures {@link GlobalOpenTelemetry} (tracing) and {@link GlobalMeterProvider} (metrics).
 * <p>
 * The hierarchy of {@link OpenTelemetrySdk} is as follows. All fields are private final and thus can only be changed via reflection
 * <p>
 * The {@link OpenTelemetrySdk} contains the {@link SdkTracerProvider}, i.e., {@link OpenTelemetrySdk#getSdkTracerProvider()}.
 * The {@link SdkTracerProvider} contains the {@link io.opentelemetry.sdk.trace.TracerSharedState}, i.e., {@link SdkTracerProvider#sharedState}.
 * The {@link io.opentelemetry.sdk.trace.TracerSharedState} contains {@link io.opentelemetry.sdk.trace.TracerSharedState#activeSpanProcessor}, which can be a list of {@link SpanProcessor} (for example {@link SimpleSpanProcessor} or {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor}.
 * <p>
 * The {@link SimpleSpanProcessor} contains the {@link SimpleSpanProcessor#spanExporter}, i.e., {@link io.opentelemetry.sdk.trace.export.SpanExporte}.
 * The {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor} contains the {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor#worker}, which then contains the {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor.Worker#spanExporter}
 * <p>
 * The hierarchy of {@link io.opentelemetry.sdk.metrics.SdkMeterProvider} is as follows. All fields are private final and thus can only be changed via reflection.
 * <p>
 * The hierarchy of {@link io.opentelemetry.sdk.metrics.SdkMeterProvider} is as follows. All fields are private final and thus can only be changed via reflection.
 * <p>
 * The {@link io.opentelemetry.sdk.metrics.SdkMeterProvider} contains {@link io.opentelemetry.sdk.metrics.SdkMeterProvider#sharedState} and {@link io.opentelemetry.sdk.metrics.SdkMeterProvider#collectionInfoMap}.
 * The {@link io.opentelemetry.sdk.metrics.internal.state.AutoValue_MeterProviderSharedState} has nothing of interest.
 * The {@link io.opentelemetry.sdk.metrics.internal.export.AutoValue_CollectionInfo} contains the {@link io.opentelemetry.sdk.metrics.internal.export.AutoValue_CollectionInfo#reader}, i.e., {@link io.opencensus.exporter.metrics.util.MetricReader}.
 * The {@link io.opentelemetry.sdk.metrics.export.MetricReaderFactory} can be implemented as {@link io.opentelemetry.sdk.metrics.export.PeriodicMetricReader}.
 * The {@link io.opentelemetry.sdk.metrics.export.PeriodicMetricReader} contains {@link io.opentelemetry.sdk.metrics.export.PeriodicMetricReader#exporter}, e.g., {@link io.opentelemetry.exporter.logging.LoggingMetricExporter}, and the {@link io.opentelemetry.sdk.metrics.export.PeriodicMetricReader#scheduledFuture}.
 * The {@link java.util.concurrent.ScheduledFuture}
 */
@Slf4j
public class OpenTelemetryControllerImpl implements IOpenTelemetryController {

    public static final String BEAN_NAME = "openTelemetryController";

    @Getter
    private boolean enabled = false;

    /**
     * Whether something in {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings} of the {@link InspectitConfig} changed
     */
    private boolean tracingSettingsChanged = false;

    /**
     * Whether something in {@link rocks.inspectit.ocelot.config.model.metrics.MetricsSettings} of the {@link InspectitConfig} changed
     */
    private boolean metricSettingsChanged = false;

    /**
     * whether {@link GlobalOpenTelemetry} and {@link GlobalMeterProvider} have successfully been configured.
     */
    @Getter
    // TODO: make sure that configured is set accordingly when OTEL is being reconfigured (false while configuring, true after success)
    private boolean configured = false;

    /**
     * Whether the {@link OpenTelemetryControllerImpl} is currently configuring and starting
     */
    private AtomicBoolean isConfiguring = new AtomicBoolean(false);

    /**
     * Returns whether the {@link OpenTelemetryControllerImpl} is currently (re-)configuring tracing and metrics
     *
     * @return Whether the {@link OpenTelemetryControllerImpl} is currently (re-)configuring tracing and metrics
     */
    public boolean isConfiguring() {
        return isConfiguring.get();
    }

    /**
     * The set of registered {@link DynamicallyActivatableTraceExporterService}.
     */
    private Set<DynamicallyActivatableTraceExporterService> registeredTraceExportServices;

    /**
     * The set of registered {@link DynamicallyActivatableMetricsExporterService}.
     */
    private Set<DynamicallyActivatableMetricsExporterService> registeredMetricExporterServices;

    private Resource serviceNameResource;

    /**
     * The custom {@link OpenTelemetryImpl}
     */
    private OpenTelemetryImpl openTelemetry;

    private MeterProviderImpl meterProvider;

    @Autowired
    InspectitEnvironment env;

    private Sampler sampler;

    @PostConstruct
    void init() {
        log.info("INIT at timestamp {}", System.currentTimeMillis());

        registeredTraceExportServices = new LinkedHashSet<>();
        registeredMetricExporterServices = new LinkedHashSet<>();

        serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, env.getCurrentConfig()
                .getServiceName()));

        // close the tracer provider when the JVM is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        enabled = true;

        Instances.openTelemetryController = this;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    synchronized private void startAtStartup(ContextRefreshedEvent event) {
        // start and configure OTEL at when the ApplicationContext gets initialized
        start();
    }

    /**
     * Configures and registers {@link io.opentelemetry.api.OpenTelemetry}, triggered by the {@link rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent} triggered
     * For tracing, the {@link SdkTracerProvider} is reconfigured and updated in the {@link GlobalOpenTelemetry}.
     * For metrics, the {@link SdkMeterProvider} is reconfigured and updated in the {@link GlobalMeterProvider}
     *
     * @return
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    // make sure this is called after the individual services have (un)-registered
    synchronized private boolean configureOpenTelemetry() {
        log.info("configureOpenTelemetry at timestamp {}", System.currentTimeMillis());
        boolean success = false;
        if (!isConfiguring.compareAndSet(false, true)) {
            log.info("Multiple configure calls");
            return true;
        }
        if (enabled) {

            InspectitConfig configuration = env.getCurrentConfig();

            // TODO: somehow compute whether anything has changed in tracing or metrics. If no changes happened, we do not need to reconfigure tracing and metrics!

            // set serviceName
            serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, configuration.getServiceName()));

            // configure tracing if not configured or when tracing settings changed
            boolean successConfigureTracing = !(tracingSettingsChanged || !configured) ? true : configureTracing(configuration);

            // configure meter provider (metrics) if not configured or when metrics settings changed
            boolean successConfigureMeterProvider = !(metricSettingsChanged || !configured) ? true : configureMeterProvider(configuration);

            if (successConfigureTracing && successConfigureMeterProvider) {
                log.info("Successfully configured OpenTelemetry with TracerProvider and MeterProvider");
            } else {
                log.error("Failed to configure OpenTelemetry. Please scan the logs for detailed failure messages.");
            }
            success = successConfigureTracing && successConfigureMeterProvider;
        }

        isConfiguring.set(false);
        // reset changed variables
        tracingSettingsChanged = false;
        metricSettingsChanged = false;
        return success;

    }

    /**
     * Shuts down the {@link SdkTracerProvider} set for {@link GlobalOpenTelemetry} and the {@link SdkMeterProvider} set for {@link GlobalMeterProvider}
     */
    @Override
    synchronized public void shutdown() {
        long start = System.nanoTime();

        if (null != openTelemetry) {
            openTelemetry.shutdown();
        }
        if (null != meterProvider) {
            long startMeterProviderShutdown = System.nanoTime();
            CompletableResultCode shutdownResult = meterProvider.shutdown();
            log.info("time to shut down {}: {} ms", meterProvider, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startMeterProviderShutdown));
        }
        GlobalMeterProvider.set(null);
        GlobalOpenTelemetry.resetForTest();
        configured = false;
        enabled = false;

        // set all OTEL related fields to null
        openTelemetry = null;
        meterProvider = null;
        serviceNameResource = null;
        sampler = null;

        log.info("Shut down {}. The shutdown process took {} ms", getClass().getSimpleName(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    synchronized public boolean start() {
        enabled = true;
        // if OTEL has not been configured (since last shutdown), configure it
        if (!configured) {
            return configured = configureOpenTelemetry();
        } else {
            return true;
        }
    }

    @Override
    synchronized public void notifyTracingSettingsChanged() {
        tracingSettingsChanged = true;
    }

    @Override
    synchronized public void notifyMetricsSettingsChanged() {
        metricSettingsChanged = true;
    }

    public static void configureAndRegisterDefault() {
        log.info("Configure and register default OpenTelemetry");

        // set up tracer provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                .setSampler(Sampler.alwaysOn())
                .build();

        // build and register OTel
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                // W3CBaggagePropagator.getInstance()
                .buildAndRegisterGlobal();
    }

    /**
     * Registers a new {@link DynamicallyActivatableService}
     *
     * @param service
     *
     * @return
     */
    // TODO: make accessible? discuss whether it is better to have this kind of catch-all method or rather individual register methods (see below)
    private boolean registerExporterService(DynamicallyActivatableService service) {
        if (service instanceof DynamicallyActivatableTraceExporterService) {
            return registerTraceExporterService((DynamicallyActivatableTraceExporterService) service);
        } else if (service instanceof DynamicallyActivatableMetricsExporterService) {
            return registerMetricExporterService((DynamicallyActivatableMetricsExporterService) service);
        } else {
            log.error("Cannot register service {}. The class is not supported. Currently supported classes are {} and  {}", service.getName(), DynamicallyActivatableTraceExporterService.class, DynamicallyActivatableMetricsExporterService.class);
            return false;
        }
    }

    /**
     * Registers a new {@link DynamicallyActivatableTraceExporterService} that is used to export {@link io.opentelemetry.sdk.trace.data.SpanData} for sampled {@link io.opentelemetry.api.trace.Span}s
     *
     * @param service
     *
     * @return
     */
    public boolean registerTraceExporterService(DynamicallyActivatableTraceExporterService service) {
        if (null == registeredTraceExportServices) {
            registeredTraceExportServices = new LinkedHashSet<>();
        }
        try {
            // try to add the service if it has not already been registered
            if (registeredTraceExportServices.add(service)) {
                notifyTracingSettingsChanged();
                log.info("The service {} was successfully registered.", service.getName());
                return true;
            } else {
                log.warn("The service {} was already registered", service.getName());
                return false;
            }
            // return configureOpenTelemetry();
        } catch (Exception e) {
            log.error("Failed to register " + service.getName(), e);
            return false;
        }

    }

    public boolean unregisterTraceExporterService(DynamicallyActivatableTraceExporterService service) {

        if (registeredTraceExportServices.remove(service)) {
            notifyTracingSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered.", service.getName());
            return false;
        }
    }

    /**
     * Register a {@link DynamicallyActivatableMetricsExporterService}
     *
     * @param service
     *
     * @return
     */
    public boolean registerMetricExporterService(DynamicallyActivatableMetricsExporterService service) {
        if (null == registeredMetricExporterServices) {
            registeredMetricExporterServices = new LinkedHashSet<>();
        }
        try {
            if (registeredMetricExporterServices.add(service)) {
                notifyMetricsSettingsChanged();
                log.info("The service {} was successfully registered.", service.getName());
                return true;
            } else {
                log.warn("The service {} was already registered!", service.getName());
                return false;
            }
            // return configureOpenTelemetry();
        } catch (Exception e) {
            log.error("Failed to register " + service.getName(), e);
            return false;
        }
    }

    public boolean unregisterMetricExporterService(DynamicallyActivatableMetricsExporterService service) {
        if (registeredMetricExporterServices.remove(service)) {
            notifyMetricsSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered.", service.getName());
            return false;
        }
    }

    private synchronized boolean configureTracing(InspectitConfig configuration) {
        if (!enabled) {
            return true;
        }
        try {
            // set up sampler
            double probability = configuration.getTracing().getSampleProbability();
            sampler = Sampler.traceIdRatioBased(probability);

            // build TracerProvider
            SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                    .setSampler(sampler)
                    .setResource(serviceNameResource);

            // add all SpanProcessors
            for (DynamicallyActivatableTraceExporterService traceExportServices : registeredTraceExportServices) {
                // TODO: or do we rather want to have a getter method in the service?
                builder.addSpanProcessor(traceExportServices.getSpanProcessor());
                log.info("addSpanProcessor for service {}", traceExportServices);
            }

            // add SpanProcessors that have been added by testing methods
            for (SpanProcessor spanProcessor : spanProcessorsForTesting) {
                builder.addSpanProcessor(spanProcessor);
            }

            // build the SdkTracerProvider
            SdkTracerProvider tracerProvider = builder.build();

            // rebuild OTel
            OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance(), W3CBaggagePropagator.getInstance(), B3Propagator.injectingMultiHeaders())))
                    // TODO: do I also need the W3CBaggagePropagator or B3Propagator?
                    // W3CBaggagePropagator.getInstance()
                    .build();

            // if the OpenTelemetryImpl has not been build, then build and register it
            if (null == openTelemetry) {
                openTelemetry = OpenTelemetryImpl.builder().openTelemetry(openTelemetrySdk).build();

                // check if any OpenTelemetry has been registered to GlobalOpenTelemetry.
                // If so, reset it.
                if (null != OpenTelemetryUtils.getGlobalOpenTelemetry()) {
                    log.info("reset {}", GlobalOpenTelemetry.get().getClass().getName());
                    GlobalOpenTelemetry.resetForTest();
                }

                // set GlobalOpenTelemetry
                openTelemetry.registerGlobal();
            }
            // otherwise, just update the underlying OpenTelemetrySdk
            else {
                openTelemetry.set(openTelemetrySdk);
            }

            // update the OTEL_TRACER field in OpenTelemetrySpanBuilderImpl
            OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();

            log.info("SpanExporter={}", Tracing.getExportComponent().getSpanExporter());
            return true;
        } catch (Exception e) {
            log.error("Failed to configure OpenTelemetry Tracing", e);
            return false;
        }
    }

    /**
     * Configures the {@link SdkMeterProvider} and registers it as the {@link GlobalMeterProvider} via {@link GlobalMeterProvider#set(MeterProvider)}
     *
     * @param configuration
     *
     * @return
     */
    private synchronized boolean configureMeterProvider(InspectitConfig configuration) {
        if (!enabled) {
            return true;
        }
        try {
            SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(serviceNameResource);

            for (DynamicallyActivatableMetricsExporterService metricsExportService : registeredMetricExporterServices) {
                log.info("add metricReader for {} ({})", metricsExportService, metricsExportService.getMetricReaderFactory());
                builder.registerMetricReader(OpenCensusMetrics.attachTo(metricsExportService.getMetricReaderFactory()));
            }

            SdkMeterProvider sdkMeterProvider = builder.build();

            // if the MeterProvider is null, build and register it
            if (null == meterProvider) {
                meterProvider = MeterProviderImpl.builder().meterProvider(sdkMeterProvider).build();
                meterProvider.registerGlobal();
            }
            // otherwise, just update the internally used SdkMeterProvider
            else {
                meterProvider.set(sdkMeterProvider);
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to configure MeterProvider", e);
            return false;
        }
    }

    /**
     * See {@link rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableSpanExporter#registerHandler(String, DynamicallyActivatableSpanExporter.Handler)}
     *
     * @param name           the name of the service
     * @param name           the name of the service handler. Must be unique for each service.
     * @param serviceHandler the service handler that is called for each ended sampled {@link io.opentelemetry.api.trace.Span}
     */
    public void registerHandler(String name, DynamicallyActivatableSpanExporter.Handler serviceHandler) {
        // TODO: implement
    }

    public void unregisterHandler(String name, DynamicallyActivatableSpanExporter.Handler serviceHandler) {

        // TODO: implement

    }

    /**
     * List of {@link SpanProcessor}.
     */
    Set<SpanProcessor> spanProcessorsForTesting = new LinkedHashSet<>();

    /**
     * Registers a {@link SpanProcessor}. ONLY USE THIS METHOD FOR TESTING!
     *
     * @param spanProcessor
     *
     * @return Whether {@link #configureTracing(InspectitConfig)} was successful, i.e., {@link GlobalOpenTelemetry} was updated
     */
    public boolean registerSpanProcessorForTesting(SpanProcessor spanProcessor) {
        spanProcessorsForTesting.add(spanProcessor);
        return configureTracing(env.getCurrentConfig());
    }

    /**
     * Unregisters a {@link SpanProcessor}. ONLY USE THIS METHOD FOR TESTING
     *
     * @param spanProcessor
     *
     * @return Whether {@link #configureTracing(InspectitConfig)} was successful, i.e., {@link GlobalOpenTelemetry} was updated
     */

    public boolean unregisterSpanProcessorForTesting(SpanProcessor spanProcessor) {
        spanProcessorsForTesting.remove(spanProcessor);
        return configureTracing(env.getCurrentConfig());
    }
}

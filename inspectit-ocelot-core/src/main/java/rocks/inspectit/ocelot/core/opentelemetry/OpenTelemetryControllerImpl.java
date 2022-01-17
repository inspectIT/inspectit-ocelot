package rocks.inspectit.ocelot.core.opentelemetry;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
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
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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
import rocks.inspectit.ocelot.core.exporter.DynamicallyActivatableTraceExporterService;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The implementation of {@link IOpenTelemetryController}. The {@link OpenTelemetryControllerImpl} configures {@link GlobalOpenTelemetry} (tracing) and {@link GlobalMeterProvider} (metrics).
 * The individual {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService services}, i.e., {@link DynamicallyActivatableMetricsExporterService} and {@link DynamicallyActivatableTraceExporterService}, register to and unregister from {@link OpenTelemetryControllerImpl this}.
 * <b>Important note:</b> {@link #shutdown() shutting down} the {@link OpenTelemetryControllerImpl} is final and cannot be revoked.
 */
@Slf4j
public class OpenTelemetryControllerImpl implements IOpenTelemetryController {

    public static final String BEAN_NAME = "openTelemetryController";

    @Getter
    private boolean enabled = false;

    @Getter
    private boolean stopped = false;

    /**
     * Whether something in {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings} or any of the {@link rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings} of the {@link InspectitConfig} changed
     */
    private boolean tracingSettingsChanged = false;

    /**
     * Whether something in {@link rocks.inspectit.ocelot.config.model.metrics.MetricsSettings} or any of the {@link rocks.inspectit.ocelot.config.model.exporters.metrics.MetricsExportersSettings} of the {@link InspectitConfig} changed
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
     * Whether the {@link OpenTelemetryImpl} is currently {@link #shutdown() shutting down}
     */
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**
     * The registered {@link DynamicallyActivatableTraceExporterService}.
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    Map<String, DynamicallyActivatableTraceExporterService> registeredTraceExportServices = new ConcurrentHashMap<>();

    /**
     * The registered {@link DynamicallyActivatableMetricsExporterService}.
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    Map<String, DynamicallyActivatableMetricsExporterService> registeredMetricExporterServices = new ConcurrentHashMap<>();

    private Resource serviceNameResource;

    /**
     * The {@link OpenTelemetryImpl} that wraps {@link OpenTelemetrySdk}
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    private OpenTelemetryImpl openTelemetry;

    /**
     * The {@link MeterProviderImpl} that wraps {@link SdkMeterProvider}
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    private MeterProviderImpl meterProvider;

    @Autowired
    InspectitEnvironment env;

    /**
     * The {@link DynamicSampler} used for tracing
     */
    private DynamicSampler sampler;

    /**
     * The {@link BatchSpanProcessor} used to process all spans
     */
    private SpanProcessor spanProcessor;

    /**
     * The {@link DynamicMultiSpanExporter} wrapper that is used to forward all spans to a list of {@link io.opentelemetry.sdk.trace.export.SpanExporter} (one for each {@link DynamicallyActivatableTraceExporterService}
     */
    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private DynamicMultiSpanExporter spanExporter;

    @PostConstruct
    @VisibleForTesting
    void init() {
        // create the service name resource
        serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, env.getCurrentConfig()
                .getServiceName()));

        initTracing(env.getCurrentConfig());

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
     * Returns whether the {@link OpenTelemetryControllerImpl} is currently (re-)configuring tracing and metrics
     *
     * @return Whether the {@link OpenTelemetryControllerImpl} is currently (re-)configuring tracing and metrics
     */
    public boolean isConfiguring() {
        return isConfiguring.get();
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
    @VisibleForTesting
    // make sure this is called after the individual services have (un)-registered
    synchronized boolean configureOpenTelemetry() {
        if (stopped) {
            return false;
        }
        boolean success = false;
        if (!isConfiguring.compareAndSet(false, true)) {
            log.info("Multiple configure calls");
            return true;
        }
        if (enabled) {

            InspectitConfig configuration = env.getCurrentConfig();

            // set serviceName
            serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, configuration.getServiceName()));

            // check if the tracing sample probability changed
            if (null == sampler || sampler.getSampleProbability() != configuration.getTracing()
                    .getSampleProbability()) {
                tracingSettingsChanged = true;
            }
            // configure tracing if not configured or when tracing settings changed
            boolean successConfigureTracing = !(tracingSettingsChanged || !configured) ? true : configureTracing(configuration);

            // configure meter provider (metrics) if not configured or when metrics settings changed
            boolean successConfigureMeterProvider = !(metricSettingsChanged || !configured) ? true : configureMeterProvider(configuration);

            success = successConfigureTracing && successConfigureMeterProvider;

            if (success) {
                log.info("Successfully configured OpenTelemetry with tracing and metrics");
            } else {
                log.error("Failed to configure OpenTelemetry. Please scan the logs for detailed failure messages.");
            }

        }

        isConfiguring.set(false);
        // reset changed variables
        tracingSettingsChanged = false;
        metricSettingsChanged = false;
        return success;
    }

    @Override
    synchronized public boolean start() {
        enabled = true;
        // if OTEL has not been configured (since last shutdown), configure it
        return configured = !configured ? configureOpenTelemetry() : true;
    }

    /**
     * Flushes the {@link #openTelemetry} and {@link #meterProvider} and waits for it to complete
     */
    @Override
    public void flush() {
        openTelemetry.flush();
        meterProvider.flush();
    }

    /**
     * Shuts down the {@link OpenTelemetryControllerImpl} by calling {@link OpenTelemetryImpl#close()} and {@link MeterProviderImpl#close()} and waits for it to complete.
     * The shutdown is final, i.e., once this {@link OpenTelemetryImpl} is shutdown, it cannot be re-enabled!
     * <p>
     * Only use this method for testing or when the JVM is shutting down.
     */
    @Override
    synchronized public void shutdown() {
        if (isStopped()) {
            return;
        }
        if (!isShuttingDown.compareAndSet(false, true)) {
            log.info("Multiple shutdown calls");
        }
        long start = System.nanoTime();

        // close tracing
        if (null != openTelemetry) {
            // note: close calls SdkTracerProvider#shutdown, which calls SpanProcessor#shutdown, which calls SpanExporter#shutdown.
            // thus, the spanProcessor and spanExporter are shut down in this process and cannot be used later
            openTelemetry.close();
        }
        // close metric provider
        if (null != meterProvider) {
            long startMeterProviderShutdown = System.nanoTime();
            // note: close calls SdkMeterProvider#shutdown, which calls MetricReader#shutdown, which calls MetricExporter#shutdown
            CompletableResultCode shutdownResult = meterProvider.close();
        }
        GlobalMeterProvider.set(null);
        GlobalOpenTelemetry.resetForTest();
        configured = false;
        enabled = false;
        stopped = true;
        isShuttingDown.set(false);

        // set all OTEL related fields to null
        openTelemetry = null;
        meterProvider = null;
        serviceNameResource = null;
        sampler = null;
        spanExporter = null;
        spanProcessor = null;

        log.info("Shut down {}. The shutdown process took {} ms", getClass().getSimpleName(), (System.nanoTime() - start) / 1000000);
    }

    @Override
    synchronized public void notifyTracingSettingsChanged() {
        tracingSettingsChanged = true;
    }

    @Override
    synchronized public void notifyMetricsSettingsChanged() {
        metricSettingsChanged = true;
    }

    /**
     * Initializes tracing components, i.e., {@link #openTelemetry}, {@link #spanExporter}, {@link #spanProcessor}, {@link #sampler} and {@link SdkTracerProvider}
     *
     * @param configuration
     */
    @VisibleForTesting
    void initTracing(InspectitConfig configuration) {
        double sampleProbability = configuration.getTracing().getSampleProbability();

        // set up sampler
        sampler = new DynamicSampler(sampleProbability);

        // set up spanProcessor and spanExporter
        spanExporter = new DynamicMultiSpanExporter();
        spanProcessor = BatchSpanProcessor.builder(spanExporter)
                .setMaxExportBatchSize(configuration.getTracing().getMaxExportBatchSize())
                .setScheduleDelay(configuration.getTracing().getScheduleDelayMillis(), TimeUnit.MILLISECONDS)
                .build();
        // build TracerProvider
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setSampler(sampler)
                .setResource(serviceNameResource)
                .addSpanProcessor(spanProcessor);

        // build the SdkTracerProvider
        SdkTracerProvider tracerProvider = builder.build();

        // build OTEL
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance(), W3CBaggagePropagator.getInstance(), B3Propagator.injectingMultiHeaders())))
                .build();

        // if the OpenTelemetryImpl has not been build, then build and register it
        openTelemetry = OpenTelemetryImpl.builder().openTelemetry(openTelemetrySdk).build();

        // check if any OpenTelemetry has been registered to GlobalOpenTelemetry.
        // If so, reset it.
        if (null != OpenTelemetryUtils.getGlobalOpenTelemetry()) {
            log.info("reset {}", GlobalOpenTelemetry.get().getClass().getName());
            GlobalOpenTelemetry.resetForTest();
        }

        // set GlobalOpenTelemetry
        openTelemetry.registerGlobal();

        // update the OTEL_TRACER field in OpenTelemetrySpanBuilderImpl in case that it was already set
        OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();
    }

    /**
     * Configures the tracing, i.e. {@link #openTelemetry} and the related {@link SdkTracerProvider}. A new {@link SdkTracerProvider} is only built once or after {@link #shutdown()} was called.
     *
     * @param configuration
     *
     * @return Whether {@link #openTelemetry} was successfully configured
     */
    @VisibleForTesting
    synchronized boolean configureTracing(InspectitConfig configuration) {
        if (!enabled || stopped) {
            return true;
        }
        try {
            // update sample probability
            sampler.setSampleProbability(configuration.getTracing().getSampleProbability());
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
    @VisibleForTesting
    synchronized boolean configureMeterProvider(InspectitConfig configuration) {
        if (!enabled) {
            return true;
        }
        try {

            // build new SdkMeterProvider
            SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(serviceNameResource);

            // register metric reader for each service
            for (DynamicallyActivatableMetricsExporterService metricsExportService : registeredMetricExporterServices.values()) {
                builder.registerMetricReader(OpenCensusMetrics.attachTo(metricsExportService.getNewMetricReaderFactory()));
            }

            SdkMeterProvider sdkMeterProvider = builder.build();

            // if the MeterProviderImpl is null, build and register it
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
     * Registers a new {@link DynamicallyActivatableTraceExporterService} that is used to export {@link io.opentelemetry.sdk.trace.data.SpanData} for sampled {@link io.opentelemetry.api.trace.Span}s
     *
     * @param service
     *
     * @return
     */
    public boolean registerTraceExporterService(DynamicallyActivatableTraceExporterService service) {
        if (null == registeredTraceExportServices) {
            registeredTraceExportServices = new ConcurrentHashMap<>();
        }
        try {
            // try to register the span exporter of the service
            if (null != spanExporter) {
                if (spanExporter.registerSpanExporter(service.getName(), service.getSpanExporter())) {
                    log.info("The spanExporter {} for the service {} was successfully registered.", service.getSpanExporter()
                            .getClass()
                            .getName(), service.getName());
                } else {
                    log.error("The spanExporter {} for the service {} was already registered", service.getSpanExporter()
                            .getClass()
                            .getName(), service.getName());
                }
            }
            // try to add the service if it has not already been registered
            if (null == registeredTraceExportServices.put(service.getName(), service)) {
                notifyTracingSettingsChanged();
                log.info("The service {} was successfully registered.", service.getName());
                return true;
            } else {
                log.warn("The service {} was already registered", service.getName());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to register " + service.getName(), e);
            return false;
        }

    }

    /**
     * Unregisters a {@link DynamicallyActivatableTraceExporterService} registered under the given name
     *
     * @param serviceName The name of the {@link DynamicallyActivatableTraceExporterService service}
     *
     * @return Whether the {@link DynamicallyActivatableTraceExporterService service} was successfully unregistered. Returns false if no service with the given name was previously registered.
     */
    private boolean unregisterTraceExporterService(String serviceName) {
        // unregister the service by removing it from the map of registered services and from the spanExporter
        // evaluates to true when a service with the  given name was previously registered
        if (null != registeredTraceExportServices.remove(serviceName) & (spanExporter == null || spanExporter.unregisterSpanExporter(serviceName))) {
            notifyTracingSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered.", serviceName);
            return false;
        }
    }

    /**
     * Unregisters a {@link DynamicallyActivatableTraceExporterService}
     *
     * @param service The {@link DynamicallyActivatableTraceExporterService}
     *
     * @return Whether the {@link DynamicallyActivatableTraceExporterService service} was successfully unregistered. Returns false if no service with the given name was previously registered.
     */
    public boolean unregisterTraceExporterService(DynamicallyActivatableTraceExporterService service) {
        return unregisterTraceExporterService(service.getName());
    }

    /**
     * Registers a {@link DynamicallyActivatableMetricsExporterService}
     *
     * @param service
     *
     * @return
     */
    public boolean registerMetricExporterService(DynamicallyActivatableMetricsExporterService service) {
        if (null == registeredMetricExporterServices) {
            registeredMetricExporterServices = new ConcurrentHashMap<>();
        }
        try {
            if (null == registeredMetricExporterServices.put(service.getName(), service)) {
                notifyMetricsSettingsChanged();
                log.info("The service {} was successfully registered.", service.getName());
                return true;
            } else {
                log.warn("The service {} was already registered!", service.getName());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to register " + service.getName(), e);
            return false;
        }
    }

    /**
     * Unregisters a {@link DynamicallyActivatableMetricsExporterService} with the given name
     *
     * @param serviceName The name of the {@link DynamicallyActivatableTraceExporterService service}
     *
     * @return Whether the {@link DynamicallyActivatableMetricsExporterService service} was successfully unregistered. Returns false if a service with the given name was already registered and has been overwritten.
     */
    private boolean unregisterMetricExporterService(String serviceName) {
        if (null != registeredMetricExporterServices.remove(serviceName)) {
            notifyMetricsSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered.", serviceName);
            return false;
        }
    }

    /**
     * Unregisters a {@link DynamicallyActivatableMetricsExporterService}
     *
     * @param service The {@link DynamicallyActivatableMetricsExporterService} to unregister
     *
     * @return Whether the {@link DynamicallyActivatableMetricsExporterService service} was successfully registered. Returns false if the service was already registered.
     */
    public boolean unregisterMetricExporterService(DynamicallyActivatableMetricsExporterService service) {
        return unregisterMetricExporterService(service.getName());
    }

}

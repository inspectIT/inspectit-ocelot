package rocks.inspectit.ocelot.core.opentelemetry;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.opencensusshim.metrics.OpenCensusMetrics;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
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
import rocks.inspectit.ocelot.core.opentelemetry.trace.RandomIdGenerator64Bit;
import rocks.inspectit.ocelot.core.utils.OpenCensusShimUtils;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The implementation of {@link IOpenTelemetryController}. The {@link OpenTelemetryControllerImpl} configures {@link GlobalOpenTelemetry}.
 * The individual {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService services} register to and unregister from {@link OpenTelemetryControllerImpl this}.
 * <b>Important note:</b> {@link #shutdown() shutting down} the {@link OpenTelemetryControllerImpl} is final and cannot be revoked.
 */
@Slf4j
public class OpenTelemetryControllerImpl implements IOpenTelemetryController {

    public static final String BEAN_NAME = "openTelemetryController";

    /**
     * Whether this {@link OpenTelemetryControllerImpl} has been shut down.
     */
    @Getter
    private boolean shutdown = false;

    /**
     * Whether something in {@link rocks.inspectit.ocelot.config.model.tracing.TracingSettings} or any of the {@link rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings} of the {@link InspectitConfig} changed
     */
    private boolean tracingSettingsChanged = false;

    /**
     * Whether something in {@link rocks.inspectit.ocelot.config.model.metrics.MetricsSettings} or any of the {@link rocks.inspectit.ocelot.config.model.exporters.metrics.MetricsExportersSettings} of the {@link InspectitConfig} changed
     */
    private boolean metricSettingsChanged = false;

    /**
     * whether {@link GlobalOpenTelemetry} has been successfully been configured and is active.
     */
    @Getter
    private boolean active = false;

    /**
     * Whether the {@link OpenTelemetryControllerImpl} is currently configuring and starting.
     */
    private AtomicBoolean isConfiguring = new AtomicBoolean(false);

    /**
     * Whether the {@link OpenTelemetryImpl} is currently {@link #shutdown() shutting down}
     */
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**
     * The registered {@link SpanExporter} of a {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service}.
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    Map<String, SpanExporter> registeredTraceExportServices = new ConcurrentHashMap<>();

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
     * The currently active {@link SdkMeterProvider}
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    private SdkMeterProvider meterProvider;

    /**
     * The currently active {@link SdkTracerProvider}
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    private SdkTracerProvider tracerProvider;

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
     * The {@link DynamicMultiSpanExporter} wrapper that is used to forward all spans to a list of {@link io.opentelemetry.sdk.trace.export.SpanExporter} (one for each {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service}
     */
    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private DynamicMultiSpanExporter multiSpanExporter;

    @PostConstruct
    @VisibleForTesting
    void init() {
        initOtel(env.getCurrentConfig());

        // close the tracer provider when the JVM is shutting down
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        Instances.openTelemetryController = this;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    synchronized private void startAtStartup(ContextRefreshedEvent event) {
        start();
    }

    /**
     * Configures and registers {@link io.opentelemetry.api.OpenTelemetry}, triggered by the {@link rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent} triggered
     * For tracing, the {@link SdkTracerProvider} is reconfigured and updated in the {@link GlobalOpenTelemetry}.
     * For metrics, the {@link SdkMeterProvider} is reconfigured and updated in the {@link GlobalOpenTelemetry}.     *
     * Using the {@link Order} annotation, we make sure this method called after the individual services have (un)-registered.
     *
     * @return
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    @VisibleForTesting
    synchronized boolean configureOpenTelemetry() {
        if (shutdown) {
            return false;
        }
        boolean success = true;
        if (!isConfiguring.compareAndSet(false, true)) {
            log.info("Multiple configure calls");
            return true;
        }

        InspectitConfig configuration = env.getCurrentConfig();

        serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, configuration.getServiceName()));

        // check if the tracing sample probability changed
        if (null == sampler || sampler.getSampleProbability() != configuration.getTracing().getSampleProbability()) {
            tracingSettingsChanged = true;
        }

        if (!active || metricSettingsChanged || tracingSettingsChanged) {

            // configure tracing if not configured or when tracing settings changed
            SdkTracerProvider sdkTracerProvider = !(tracingSettingsChanged || !active) ? tracerProvider : configureTracing(configuration);

            // configure meter provider (metrics) if not configured or when metrics settings changed
            SdkMeterProvider sdkMeterProvider = !(metricSettingsChanged || !active) ? meterProvider : configureMeterProvider();

            // only if metrics settings changed or OTEL has not been configured and is running, we need to rebuild the OpenTelemetrySdk
            if (metricSettingsChanged || !active) {
                OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                        .setTracerProvider(sdkTracerProvider)
                        .setMeterProvider(sdkMeterProvider)
                        .build();
                openTelemetry.set(openTelemetrySdk, false, false);
            }
            success = null != sdkMeterProvider && null != sdkTracerProvider;
            meterProvider = sdkMeterProvider;
            tracerProvider = sdkTracerProvider;
        }

        if (success) {
            log.info("Successfully configured OpenTelemetry with tracing and metrics");
        } else {
            log.error("Failed to configure OpenTelemetry. Please scan the logs for detailed failure messages.");
        }

        isConfiguring.set(false);
        tracingSettingsChanged = false;
        metricSettingsChanged = false;
        return success;
    }

    @Override
    synchronized public boolean start() {
        // if OTEL is not already up and running, configure and start it
        if (active) {
            throw new IllegalStateException("The OpenTelemetry controller is already running and cannot be started again.");
        } else {
            active = configureOpenTelemetry();
            return active;
        }
    }

    /**
     * Flushes all pending spans ({@link #openTelemetry}) and metrics ({@link #meterProvider}) and waits for it to complete.
     */
    @Override
    public void flush() {
        openTelemetry.flush();
    }

    /**
     * Shuts down the {@link OpenTelemetryControllerImpl} by calling {@link OpenTelemetryImpl#close()} and waits for it to complete.
     * The shutdown is final, i.e., once this {@link OpenTelemetryImpl} is shutdown, it cannot be restarted!
     * <p>
     * Only use this method for testing or when the JVM is shutting down.
     */
    @Override
    synchronized public void shutdown() {
        if (isShutdown()) {
            return;
        }
        if (!isShuttingDown.compareAndSet(false, true)) {
            log.info("Multiple shutdown calls");
            return;
        }
        long start = System.nanoTime();

        // close OTEL
        if (null != openTelemetry) {
            // note: close calls SdkTracerProvider#shutdown, which calls SpanProcessor#shutdown, which calls SpanExporter#shutdown.
            // thus, the spanProcessor and spanExporter are shut down in this process and cannot be used later
            // note: also close calls SdkMeterProvider#shutdown, which calls MetricReader#shutdown, which calls MetricExporter#shutdown
            openTelemetry.close();
        }

        GlobalOpenTelemetry.resetForTest();
        active = false;
        shutdown = true;
        isShuttingDown.set(false);

        // set all OTEL related fields to null
        openTelemetry = null;
        meterProvider = null;
        serviceNameResource = null;
        sampler = null;
        multiSpanExporter = null;
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
     * Initializes tracer and meter provider components, i.e., {@link #openTelemetry}, {@link #multiSpanExporter}, {@link #spanProcessor}, {@link #sampler}, {@link SdkTracerProvider}, and {@link SdkMeterProvider}
     *
     * @param configuration
     */
    @VisibleForTesting
    void initOtel(InspectitConfig configuration) {

        serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, env.getCurrentConfig()
                .getServiceName()));
        tracerProvider = buildTracerProvider(configuration);
        meterProvider = SdkMeterProvider.builder().setResource(serviceNameResource).build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        openTelemetry = new OpenTelemetryImpl(openTelemetrySdk);

        // if any OpenTelemetry has already been registered to GlobalOpenTelemetry, reset it.
        if (null != OpenTelemetryUtils.getGlobalOpenTelemetry()) {
            // we need to reset it before we can register our custom OpenTelemetryImpl, as GlobalOpenTelemetry is throwing an exception if we want to register a new OpenTelemetry if a previous one is still registered.
            log.info("Reset previously registered GlobalOpenTelemetry ({}) during the initialization of {} to register {}", GlobalOpenTelemetry
                    .get()
                    .getClass()
                    .getName(), getName(), openTelemetry.getClass().getSimpleName());
            GlobalOpenTelemetry.resetForTest();
        }
        openTelemetry.registerGlobal();

        // update the OTEL_TRACER field in OpenTelemetrySpanBuilderImpl in case that it was already set
        OpenCensusShimUtils.updateOpenTelemetryTracerInOpenTelemetrySpanBuilderImpl();
    }

    /**
     * Builds a new {@link SdkTracerProvider} based on the given {@link InspectitConfig}
     *
     * @param configuration
     *
     * @return A new {@link SdkTracerProvider} based on the {@link InspectitConfig}
     */
    private SdkTracerProvider buildTracerProvider(InspectitConfig configuration) {
        double sampleProbability = configuration.getTracing().getSampleProbability();
        sampler = new DynamicSampler(sampleProbability);
        multiSpanExporter = DynamicMultiSpanExporter.create();
        spanProcessor = BatchSpanProcessor.builder(multiSpanExporter)
                .setMaxExportBatchSize(configuration.getTracing().getMaxExportBatchSize())
                .setScheduleDelay(configuration.getTracing().getScheduleDelayMillis(), TimeUnit.MILLISECONDS)
                .build();
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setSampler(sampler)
                .setResource(serviceNameResource)
                .addSpanProcessor(spanProcessor);

        if (env.getCurrentConfig().getTracing().isUse64BitTraceIds()) {
            log.info("Use of trace IDs with a length of 64 bits.");
            builder.setIdGenerator(RandomIdGenerator64Bit.INSTANCE);
        } else {
            log.info("Use of trace IDs with the default length (128 bits).");
        }

        return builder.build();
    }

    /**
     * Configures the tracing, i.e. {@link #openTelemetry} and the related {@link SdkTracerProvider}. A new {@link SdkTracerProvider} is only built once or after {@link #shutdown()} was called.
     *
     * @param configuration The {@link InspectitConfig} used to build the {@link SdkTracerProvider}
     *
     * @return The updated {@link SdkTracerProvider} or null if the configuration failed.
     */
    @VisibleForTesting
    synchronized SdkTracerProvider configureTracing(InspectitConfig configuration) {
        if (shutdown) {
            return null;
        }
        try {
            sampler.setSampleProbability(configuration.getTracing().getSampleProbability());
            return tracerProvider;

        } catch (Exception e) {
            log.error("Failed to configure OpenTelemetry Tracing", e);
            return null;
        }
    }

    /**
     * Configures the {@link SdkMeterProvider}
     *
     * @return The updated {@link SdkMeterProvider} or null if the configuration failed.
     */
    @VisibleForTesting
    synchronized SdkMeterProvider configureMeterProvider() {
        if (shutdown) {
            return null;
        }
        try {

            // stop the previously registered MeterProvider
            if (null != meterProvider) {
                OpenTelemetryUtils.stopMeterProvider(meterProvider, true);
            }
            SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(serviceNameResource);

            // register metric reader for each service
            for (DynamicallyActivatableMetricsExporterService metricsExportService : registeredMetricExporterServices.values()) {
                builder.registerMetricReader(OpenCensusMetrics.attachTo(metricsExportService.getNewMetricReaderFactory()));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to configure MeterProvider", e);
            return null;
        }
    }

    /**
     * Registers a new {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service} that is used to export {@link io.opentelemetry.sdk.trace.data.SpanData} for sampled {@link io.opentelemetry.api.trace.Span}s
     *
     * @param spanExporter The {@link SpanExporter} of the {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service}
     * @param serviceName  The name of the trace exporter service
     *
     * @return Whether the registration was successful
     */
    public boolean registerTraceExporterService(SpanExporter spanExporter, String serviceName) {
        try {
            // try to register the span exporter of the service
            if (null != multiSpanExporter) {
                if (multiSpanExporter.registerSpanExporter(serviceName, spanExporter)) {
                    log.info("The spanExporter {} for the service {} was successfully registered.", spanExporter.getClass()
                            .getName(), serviceName);
                } else {
                    log.error("The spanExporter {} for the service {} was already registered", spanExporter.getClass()
                            .getName(), serviceName);
                }
            }
            // try to add the service if it has not already been registered
            if (null == registeredTraceExportServices.put(serviceName, spanExporter)) {
                notifyTracingSettingsChanged();
                log.info("The service {} was successfully registered.", serviceName);
                return true;
            } else {
                log.warn("The service {} was already registered", serviceName);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to register " + serviceName, e);
            return false;
        }

    }

    /**
     * Unregisters a {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service} registered under the given name.
     * For this, the {@link SpanExporter} of the {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService} is removed from {@link #registeredTraceExportServices} and {@link #multiSpanExporter}.
     *
     * @param serviceName The name of the {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service}
     *
     * @return Whether the {@link rocks.inspectit.ocelot.core.service.DynamicallyActivatableService trace exporter service} was successfully unregistered. Returns false if no service with the given name was previously registered
     */
    public boolean unregisterTraceExporterService(String serviceName) {
        if (null != registeredTraceExportServices.remove(serviceName) & (multiSpanExporter == null || multiSpanExporter.unregisterSpanExporter(serviceName))) {
            notifyTracingSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered.", serviceName);
            return false;
        }
    }

    /**
     * Registers a {@link DynamicallyActivatableMetricsExporterService}
     *
     * @param service The {@link DynamicallyActivatableMetricsExporterService} to register
     *
     * @return Whether the {@link DynamicallyActivatableMetricsExporterService} was successfully registered
     */
    public boolean registerMetricExporterService(DynamicallyActivatableMetricsExporterService service) {
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
     * Unregisters a {@link DynamicallyActivatableMetricsExporterService} with the given name.
     *
     * @param serviceName The name of the {@link DynamicallyActivatableMetricsExporterService service}
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

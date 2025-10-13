package rocks.inspectit.ocelot.core.opentelemetry;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ServiceAttributes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.bootstrap.Instances;
import rocks.inspectit.ocelot.bootstrap.opentelemetry.IOpenTelemetryController;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.exporters.metrics.MetricsExportersSettings;
import rocks.inspectit.ocelot.config.model.exporters.trace.TraceExportersSettings;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.tracing.TracingSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.exporter.MetricReaderProvider;
import rocks.inspectit.ocelot.core.opentelemetry.events.OpenTelemetryConfiguredEvent;
import rocks.inspectit.ocelot.core.opentelemetry.metrics.ViewManager;
import rocks.inspectit.ocelot.core.opentelemetry.resource.ResourceAttributesProvider;
import rocks.inspectit.ocelot.core.opentelemetry.trace.CustomIdGenerator;
import rocks.inspectit.ocelot.core.opentelemetry.trace.samplers.DynamicSampler;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// Sorry for this very dirty class...
// Metrics and tracing should be outsourced into new classes

/**
 * The implementation of {@link IOpenTelemetryController}. The {@link OpenTelemetryControllerImpl} configures {@link GlobalOpenTelemetry}.
 * The individual {@link DynamicallyActivatableService services} register to and unregister from {@link OpenTelemetryControllerImpl this}.
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
     * Whether something in {@link TracingSettings} or any of the {@link TraceExportersSettings} of the {@link InspectitConfig} changed
     */
    private boolean tracingSettingsChanged = false;

    /**
     * Whether something in {@link MetricsSettings} or any of the {@link MetricsExportersSettings} of the {@link InspectitConfig} changed
     */
    private boolean metricSettingsChanged = false;

    /**
     * Whether some
     */
    private boolean viewsChanged = false;

    /**
     * Whether {@link GlobalOpenTelemetry} has been successfully been configured and is active.
     */
    @Getter
    private boolean active = false;

    /**
     * Whether the {@link OpenTelemetryControllerImpl} is currently configuring and starting.
     */
    private final AtomicBoolean isConfiguring = new AtomicBoolean(false);

    /**
     * Whether the {@link OpenTelemetryImpl} is currently {@link #shutdown() shutting down}
     */
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**
     * The registered {@link SpanExporter} of a {@link DynamicallyActivatableService trace exporter service}.
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    Map<String, SpanExporter> registeredTraceExportServices = new ConcurrentHashMap<>();

    /**
     * The registered services, which provide new metric readers.
     * Since we need to build new metric readers for every SDK update, we cannot store metric readers directly.
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    Map<String, MetricReaderProvider> registeredMetricReaderProviders = new ConcurrentHashMap<>();

    /**
     * The registered {@link MetricProducer}
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    Set<MetricProducer> registeredMetricProducer = new HashSet<>();

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
    @VisibleForTesting
    InspectitEnvironment env;

    @Autowired
    @VisibleForTesting
    ApplicationEventPublisher eventPublisher;

    @Autowired
    @VisibleForTesting
    ViewManager viewManager;

    @Autowired
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    CustomIdGenerator idGenerator;

    /**
     * The {@link DynamicSampler} used for tracing
     */
    @VisibleForTesting
    @Getter(AccessLevel.PACKAGE)
    private DynamicSampler sampler;

    /**
     * The {@link BatchSpanProcessor} used to process all spans
     */
    @Getter(AccessLevel.PACKAGE)
    private SpanProcessor spanProcessor;

    /**
     * The {@link DynamicMultiSpanExporter} wrapper that is used to forward all spans to a list of {@link SpanExporter}
     * (one for each {@link DynamicallyActivatableService trace exporter service}
     */
    @VisibleForTesting
    @Setter(AccessLevel.PACKAGE)
    private DynamicMultiSpanExporter multiSpanExporter;

    /**
     * {@link Resource} containing tracer provider attributes
     */
    @Getter(AccessLevel.PACKAGE)
    private Resource tracerProviderResource;

    @PostConstruct
    @VisibleForTesting
    void init() {
        initializeOpenTelemetry(env.getCurrentConfig());
        Instances.openTelemetryController = this;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order()
    synchronized private void startAtStartup() {
        start();
    }

    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleContextClosed() {
        flush();
        shutdown();
    }

    @Override
    synchronized public boolean start() {
        // if OTel is not already up and running, configure and start it
        if (active) {
            throw new IllegalStateException("The OpenTelemetry controller is already running and cannot be started again.");
        } else {
            active = configureOpenTelemetry();
            return active;
        }
    }

    /**
     * Initializes tracer and meter provider components but does not set {@link #active}!
     */
    @VisibleForTesting
    void initializeOpenTelemetry(InspectitConfig configuration) {
        meterProvider = getMeterProviderBuilder(configuration).build();
        tracerProvider = getTracerProviderBuilder(configuration).build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        openTelemetry = new OpenTelemetryImpl(openTelemetrySdk);

        // if any OpenTelemetry has already been registered to GlobalOpenTelemetry, reset it.
        if (null != OpenTelemetryUtils.getGlobalOpenTelemetry()) {
            // we need to reset it before we can register our custom OpenTelemetryImpl, as GlobalOpenTelemetry is throwing an exception if we want to register a new OpenTelemetry if a previous one is still registered.
            log.info("Reset previously registered GlobalOpenTelemetry ({}) during the initialization of {} to register {}",
                    GlobalOpenTelemetry.get().getClass().getName(),
                    getName(),
                    openTelemetry.getClass().getSimpleName());
            // currently, this is the only existing method to reset GlobalOpenTelemetry during runtime
            GlobalOpenTelemetry.resetForTest();
        }
        openTelemetry.registerGlobal();
    }

    /**
     * Configures and registers {@link OpenTelemetry}, triggered by the {@link InspectitConfigChangedEvent} triggered
     * For tracing, the {@link SdkTracerProvider} is reconfigured and updated in the {@link GlobalOpenTelemetry}.
     * For metrics, the {@link SdkMeterProvider} is reconfigured and updated in the {@link GlobalOpenTelemetry}.
     * Using the {@link Order} annotation, we make sure this method called after the individual services have (un)-registered.
     *
     * @return true, if OpenTelemetry was successfully configured
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @Order()
    @VisibleForTesting
    synchronized boolean configureOpenTelemetry() {
        if (shutdown) return false;

        boolean success = true;
        if (!isConfiguring.compareAndSet(false, true)) {
            log.info("Multiple configure OpenTelemetry calls");
            return true;
        }

        InspectitConfig configuration = env.getCurrentConfig();

        // check if the tracing sample probability changed
        if (null == sampler || sampler.getSampleProbability() != configuration.getTracing().getSampleProbability()) {
            tracingSettingsChanged = true;
        }

        viewsChanged = viewManager.shouldUpdateViews();
        boolean configurationChanged = !active || metricSettingsChanged || tracingSettingsChanged || viewsChanged;
        boolean updateMetrics = metricSettingsChanged || viewsChanged || !active;

        if (configurationChanged) {

            // configure tracing if not configured or when tracing settings changed
            SdkTracerProvider sdkTracerProvider = !(tracingSettingsChanged || !active) ? tracerProvider : configureTracerProvider(configuration);

            // configure meter provider (metrics) if not configured or when metrics settings changed
            SdkMeterProvider sdkMeterProvider = !updateMetrics ? meterProvider : configureMeterProvider();

            // only if metrics/views settings changed or OTel has not been configured and is running, we need to rebuild the OpenTelemetrySdk
            if (metricSettingsChanged || viewsChanged || !active) {
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
            log.error("Failed to configure OpenTelemetry. Please scan the logs for detailed failure messages");
        }

        OpenTelemetryConfiguredEvent event = new OpenTelemetryConfiguredEvent(this, success, updateMetrics);
        eventPublisher.publishEvent(event);

        isConfiguring.set(false);
        tracingSettingsChanged = false;
        metricSettingsChanged = false;
        viewsChanged = false;

        return success;
    }

    /**
     * Flushes all pending spans ({@link #openTelemetry}) and metrics ({@link #meterProvider}) and waits for it to complete.
     */
    @Override
    public void flush() {
        log.info("Flush pending OTel data");
        long start = System.nanoTime();
        openTelemetry.flush();
        log.info("Flushing process took {} ms", (System.nanoTime() - start) / 1000000);
    }

    /**
     * Shuts down the {@link OpenTelemetryControllerImpl} by calling {@link OpenTelemetryImpl#close()} and waits for it to complete.
     * The shutdown is final, i.e., once this {@link OpenTelemetryImpl} is shutdown, it cannot be restarted!
     * <p>
     * Only use this method for testing or when the JVM is shutting down.
     */
    @Override
    synchronized public void shutdown() {
        if (isShutdown()) return;

        if (!isShuttingDown.compareAndSet(false, true)) {
            log.info("Multiple shutdown calls");
            return;
        }
        long start = System.nanoTime();

        // close OTel
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

        // set all OTel related fields to null
        openTelemetry = null;
        meterProvider = null;
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
        // we have to update the SDK when the metric readers change, if already active
        if (active) {
            configureOpenTelemetry();
        }
    }

    /**
     * @return A new {@link SdkTracerProviderBuilder} based on the {@link InspectitConfig}
     */
    private SdkTracerProviderBuilder getTracerProviderBuilder(InspectitConfig configuration) {
        sampler = new DynamicSampler(configuration.getTracing().getSampleMode(), configuration.getTracing()
                .getSampleProbability());
        tracerProviderResource = getTracerProviderResource(configuration);
        multiSpanExporter = DynamicMultiSpanExporter.create();
        spanProcessor = BatchSpanProcessor.builder(multiSpanExporter)
                .setMaxExportBatchSize(configuration.getTracing().getMaxExportBatchSize())
                .setScheduleDelay(configuration.getTracing().getScheduleDelayMillis(), TimeUnit.MILLISECONDS)
                .build();

        return SdkTracerProvider.builder()
                .setSampler(sampler)
                .setResource(tracerProviderResource)
                .addSpanProcessor(spanProcessor)
                .setIdGenerator(idGenerator);
    }

    /**
     * Gets a {@link Resource} for the tracer provider attributes.
     */
    private static Resource getTracerProviderResource(InspectitConfig configuration) {
        AttributesBuilder builder = Attributes.builder();

        builder.put(ServiceAttributes.SERVICE_NAME, configuration.getExporters().getTracing().getServiceName());
        builder.putAll(ResourceAttributesProvider.getTracerProviderResourceAttributes());

        return Resource.create(builder.build());
    }

    /**
     * @return A new {@link SdkMeterProviderBuilder} based on the {@link InspectitConfig}
     */
    private SdkMeterProviderBuilder getMeterProviderBuilder(InspectitConfig configuration) {
        AttributesBuilder builder = Attributes.builder();
        builder.put( ServiceAttributes.SERVICE_NAME, configuration.getServiceName());
        builder.putAll(ResourceAttributesProvider.getMeterProviderResourceAttributes());
        Resource metricResource = Resource.create(builder.build());

        return SdkMeterProvider.builder().setResource(metricResource);
    }

    /**
     * (Re-) Configures the tracing. Currently, only the sampleProbability has to be updatable at runtime.
     *
     * @param configuration The current {@link InspectitConfig}
     *
     * @return The updated {@link SdkTracerProvider} or null if the configuration failed.
     */
    @VisibleForTesting
    synchronized SdkTracerProvider configureTracerProvider(InspectitConfig configuration) {
        if (shutdown) return null;

        try {
            sampler.setSampler(configuration.getTracing().getSampleMode(),
                    configuration.getTracing().getSampleProbability());
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
        if (shutdown) return null;

        try {
            // stop the previously registered MeterProvider
            if (null != meterProvider) {
                OpenTelemetryUtils.stopMeterProvider(meterProvider, true);
            }

            SdkMeterProviderBuilder builder = getMeterProviderBuilder(env.getCurrentConfig());

            // register metric views
            Map<InstrumentSelector, View> toBeRegisteredViews = viewManager.processViews(viewsChanged);
            for (val entry : toBeRegisteredViews.entrySet()) {
                InstrumentSelector selector = entry.getKey();
                View view = entry.getValue();
                builder.registerView(selector, view);
            }

            // register metric reader for each service
            if (!CollectionUtils.isEmpty(registeredMetricReaderProviders)) {
                for (MetricReaderProvider provider : registeredMetricReaderProviders.values()) {
                    builder.registerMetricReader(provider.getNewMetricReader());
                }
            }
            else {
                log.info("OpenTelemetry has not registered any MetricReader! " +
                        "Thus no metrics can be recorded. Enable at least one metrics exporter to record metrics");
            }

            // register additional metric producers
            for (MetricProducer producer : registeredMetricProducer) {
                builder.registerMetricProducer(producer);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to configure MeterProvider", e);
            return null;
        }
    }

    @VisibleForTesting
    @Override
    public boolean registerTraceExporterService(Object spanExporter, String serviceName) {
        if (!(spanExporter instanceof SpanExporter)) {
            throw new RuntimeException(String.format("Cannot register trace exporter service. The object '%s' is not instance of '%s'", spanExporter.getClass(), SpanExporter.class));
        }
        return registerTraceExporterService((SpanExporter) spanExporter, serviceName);
    }

    @Override
    public boolean registerMetricReader(Object metricReader, String serviceName) {
        if (!(metricReader instanceof MetricReader)) {
            throw new RuntimeException(String.format("Cannot register metric reader. The object '%s' is not instance of '%s'", metricReader.getClass(), MetricReader.class));
        }
        MetricReaderProvider provider = () -> (MetricReader) metricReader;
        return registerMetricReaderProvider(provider, serviceName);
    }

    /**
     * Registers a new {@link DynamicallyActivatableService trace exporter service} that is used to export {@link io.opentelemetry.sdk.trace.data.SpanData} for sampled {@link io.opentelemetry.api.trace.Span}s
     *
     * @param spanExporter The {@link SpanExporter} of the {@link DynamicallyActivatableService trace exporter service}
     * @param serviceName  The name of the trace exporter service
     *
     * @return Whether the registration was successful
     */
    public boolean registerTraceExporterService(SpanExporter spanExporter, String serviceName) {
        try {
            // try to register the span exporter of the service
            if (null != multiSpanExporter) {
                if (multiSpanExporter.registerSpanExporter(serviceName, spanExporter)) {
                    log.info("The spanExporter {} for the service {} was successfully registered", spanExporter.getClass()
                            .getName(), serviceName);
                } else {
                    log.error("The spanExporter {} for the service {} was already registered", spanExporter.getClass()
                            .getName(), serviceName);
                }
            }
            // try to add the service if it has not already been registered
            if (null == registeredTraceExportServices.put(serviceName, spanExporter)) {
                notifyTracingSettingsChanged();
                log.info("The service {} was successfully registered", serviceName);
                return true;
            } else {
                log.warn("The service {} was already registered", serviceName);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to register {}", serviceName, e);
            return false;
        }
    }

    /**
     * Unregisters a {@link DynamicallyActivatableService trace exporter service} registered under the given name.
     * For this, the {@link SpanExporter} of the {@link DynamicallyActivatableService} is removed from {@link #registeredTraceExportServices} and {@link #multiSpanExporter}.
     *
     * @param serviceName The name of the {@link DynamicallyActivatableService trace exporter service}
     *
     * @return Whether the {@link DynamicallyActivatableService trace exporter service} was successfully unregistered. Returns false if no service with the given name was previously registered
     */
    public boolean unregisterTraceExporterService(String serviceName) {
        if (null != registeredTraceExportServices.remove(serviceName) & (multiSpanExporter == null || multiSpanExporter.unregisterSpanExporter(serviceName))) {
            notifyTracingSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered", serviceName);
            return false;
        }
    }

    /**
     * Registers a DynamicallyActivatableMetricsExporterService, which provides a {@link MetricReader}
     *
     * @param provider The provider for {@link MetricReader metric readers}
     *
     * @return Whether the provider was successfully registered
     */
    public boolean registerMetricReaderProvider(MetricReaderProvider provider, String serviceName) {
        try {
            if (null == registeredMetricReaderProviders.put(serviceName, provider)) {
                log.info("The service {} was successfully registered", serviceName);
                notifyMetricsSettingsChanged();
                return true;
            } else {
                log.warn("The service {} was already registered!", serviceName);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to register {}", serviceName, e);
            return false;
        }
    }

    /**
     * Unregisters a {@link MetricReader} with the given name.
     *
     * @param serviceName The name of the {@link MetricReader service}
     *
     * @return Whether the {@link MetricReader service} was successfully unregistered. Returns false if a service with the given name was already registered and has been overwritten.
     */
    public boolean unregisterMetricExporterService(String serviceName) {
        if (null != registeredMetricReaderProviders.remove(serviceName)) {
            notifyMetricsSettingsChanged();
            return true;
        } else {
            log.warn("Failed to unregister {}. The service has not been registered", serviceName);
            return false;
        }
    }

    /**
     * @return Whether the {@link MetricProducer producer} was successfully registered
     */
    public boolean registerMetricProducer(MetricProducer producer) {
        return registeredMetricProducer.add(producer);
    }
}

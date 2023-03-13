package rocks.inspectit.ocelot.core.metrics;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.stats.*;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.Tags;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.internal.descriptor.InstrumentDescriptor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.percentiles.PercentileViewManager;
import rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryConfiguredEvent;
import rocks.inspectit.ocelot.core.opentelemetry.OpenTelemetryControllerImpl;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;
import rocks.inspectit.ocelot.core.utils.OpenTelemetryUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class is responsible for creating and caching OpenCensus views and measures
 * based on what is defined in inspectit.metrics.definitions.
 */
@Component
@Slf4j
public class MeasuresAndViewsManager {

    @Autowired
    private ViewManager viewManager;

    @Autowired
    private StatsRecorder statsRecorder;

    @Autowired
    private CommonTagsManager commonTags;

    @Autowired
    private PercentileViewManager percentileViewManager;

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private OpenTelemetryControllerImpl openTelemetryController;

    /**
     * Caches all created measures.
     */
    private final ConcurrentHashMap<String, Measure> cachedMeasures = new ConcurrentHashMap<>();

    /**
     * Caches all created {@link io.opentelemetry.sdk.metrics.AbstractInstrument}.
     * As we cannot access the {@link io.opentelemetry.sdk.metrics.AbstractInstrument}, we store it as an {@link Object}
     */
    private final ConcurrentHashMap<String, Map.Entry<Object, InstrumentDescriptor>> cachedInstruments = new ConcurrentHashMap<>();

    /**
     * Caches the definition which was used to build the measures and views for a given metric.
     * This is used to quickly detect which metrics have changed on configuration updates.
     */
    private final Map<String, MetricDefinitionSettings> currentMetricDefinitionSettings = new HashMap<>();

    /**
     * If a measure with the given name is defined via {@link MetricsSettings#getDefinitions()},
     * it is returned by this method.
     * You should not cache the result of this method to make sure that dynamic updates are not missed.
     *
     * @param name the name of the measure (=the name of the {@link MetricDefinitionSettings}
     *
     * @return the measure if it is registered, an empty optional otherwise
     */
    public Optional<Measure> getMeasure(String name) {
        return Optional.ofNullable(cachedMeasures.get(name));
    }

    /**
     * If a measure with type long an given name is defined via {@link MetricsSettings#getDefinitions()},
     * it is returned by this method.
     * You should not cache the result of this method to make sure that dynamic updates are not missed.
     *
     * @param name the name of the measure (=the name of the {@link MetricDefinitionSettings}
     *
     * @return the measure if it is registered and has type long, an empty optional otherwise
     */
    public Optional<Measure.MeasureLong> getMeasureLong(String name) {
        val measure = cachedMeasures.get(name);
        if (measure instanceof Measure.MeasureLong) {
            return Optional.of((Measure.MeasureLong) measure);
        }
        return Optional.empty();
    }

    /**
     * If a measure with type double an given name is defined via {@link MetricsSettings#getDefinitions()},
     * it is returned by this method.
     * You should not cache the result of this method to make sure that dynamic updates are not missed.
     *
     * @param name the name of the measure (=the name of the {@link MetricDefinitionSettings}
     *
     * @return the measure if it is registered and has type double, an empty optional otherwise
     */
    public Optional<Measure.MeasureDouble> getMeasureDouble(String name) {
        val measure = cachedMeasures.get(name);
        if (measure instanceof Measure.MeasureDouble) {
            return Optional.of((Measure.MeasureDouble) measure);
        }
        return Optional.empty();
    }

    /**
     * If an {@link io.opentelemetry.sdk.metrics.AbstractInstrument} with the given name is defined via {@link MetricsSettings#getDefinitions()},
     * it is returned by this method.
     * You should not cache the result of this method to make sure that dynamic updates are not missed.
     *
     * @param name the name of the instrument (=the name of the {@link MetricDefinitionSettings}
     *
     * @return the instrument if it is registered, an empty optional otherwise
     */
    public Optional<Object> getInstrument(String name) {
        return Optional.ofNullable(cachedInstruments.get(name).getKey());
    }

    /**
     * If an instrument with type double and the given name is defined via {@link MetricsSettings#getDefinitions()},
     * it is returned by this method.
     * You should not cache the result of this method to make sure that dynamic updates are not missed.
     *
     * @param name the name of the measure (=the name of the {@link MetricDefinitionSettings}
     *
     * @return the instrument if it is registered and has type double, an empty optional otherwise
     */
    public Optional<DoubleHistogram> getInstrumentDouble(String name) {
        Object instrument = cachedInstruments.get(name).getKey();
        if (instrument instanceof DoubleHistogram) {
            return Optional.of((DoubleHistogram) instrument);
        }
        return Optional.empty();
    }

    /**
     * If an instrument with type long and the given name is defined via {@link MetricsSettings#getDefinitions()},
     * it is returned by this method.
     * You should not cache the result of this method to make sure that dynamic updates are not missed.
     *
     * @param name the name of the measure (=the name of the {@link MetricDefinitionSettings}
     *
     * @return the instrument if it is registered and has type long, an empty optional otherwise
     */
    public Optional<LongHistogram> getInstrumentLong(String name) {
        Object measure = cachedInstruments.get(name).getKey();
        if (measure instanceof LongHistogram) {
            return Optional.of((LongHistogram) measure);
        }
        return Optional.empty();
    }

    /**
     * Records a measurement for the given measure, if it exists.
     * Depending on the measure type either {@link Number#doubleValue()}
     * or {@link Number#longValue()} is used.
     *
     * @param measureName the name of the measure
     * @param value       the measurement value for this measure
     */
    public void tryRecordingMeasurement(String measureName, Number value) {
        tryRecordingMeasurement(measureName, value, Tags.getTagger().getCurrentTagContext());
    }

    /**
     * Records a measurement for the given measure, if it exists.
     * Depending on the measure type either {@link Number#doubleValue()} or {@link Number#longValue()} is used.
     *
     * @param measureName the name of the measure
     * @param value       the measurement value for this measure
     * @param tags        the tags to add to the measurement
     */
    public void tryRecordingMeasurement(String measureName, Number value, TagContext tags) {
        val measure = getMeasure(measureName);
        if (measure.isPresent()) {
            val m = measure.get();
            if (m instanceof Measure.MeasureLong) {
                MeasureMap result = statsRecorder.newMeasureMap();
                result.put((Measure.MeasureLong) m, value.longValue());
                result.record(tags);
            } else if (m instanceof Measure.MeasureDouble) {
                MeasureMap result = statsRecorder.newMeasureMap();
                result.put((Measure.MeasureDouble) m, value.doubleValue());
                result.record(tags);
            }
        }
        percentileViewManager.recordMeasurement(measureName, value.doubleValue(), tags);
    }

    /**
     * Records a measurement for the given instrument, if it exists.
     * Depending on the instrument type either {@link Number#doubleValue()} or {@link Number#longValue()} is used.
     *
     * @param measureName the name of the instrument
     * @param value       the measurement value for this instrument
     * @param attributes  the list of attributes to add to the measurement
     */
    public void tryRecordingMeasurement(String measureName, Number value, Attributes attributes) {
        Optional<Object> optMeasurement = getInstrument(measureName);
        if (optMeasurement.isPresent()) {
            Object measurement = optMeasurement.get();
            if (measurement instanceof DoubleHistogram) {
                ((DoubleHistogram) measurement).record(value.doubleValue(), attributes);
            } else if (measurement instanceof LongHistogram) {
                ((LongHistogram) measurement).record(value.longValue(), attributes);
            }
        }
        percentileViewManager.recordMeasurement(measureName, value.doubleValue(), attributes);
    }

    /**
     * Creates the measures and views defined via {@link MetricsSettings#getDefinitions()}.
     * OpenCensus does currently not allow the removal of views, therefore updating metrics is not possible.
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @Order(CommonTagsManager.CONFIG_EVENT_LISTENER_ORDER_PRIORITY + 1) //to ensure common tags are updated first
    @PostConstruct
    public void updateMetricDefinitions() {
        MetricsSettings metricsSettings = env.getCurrentConfig().getMetrics();
        if (metricsSettings.isEnabled()) {
            val newMetricDefinitions = metricsSettings.getDefinitions();

            newMetricDefinitions.forEach((name, def) -> {
                val defWithDefaults = def.getCopyWithDefaultsPopulated(name, metricsSettings.getFrequency());
                val oldDef = currentMetricDefinitionSettings.get(name);
                if (defWithDefaults.isEnabled() && !defWithDefaults.equals(oldDef)) {
                    addOrUpdateAndCacheMeasureWithViews(name, defWithDefaults);
                }
            });
        }
        //TODO: delete removed measures and views as soon as this is possible in Open-Census
    }

    /**
     * Tries to create a measure based on the given definition, with checking measures and views reported by {@link #viewManager}.
     * <p>
     * If the measure or a view already exists, info messages are printed out
     *
     * @param measureName the name of the measure
     * @param definition  the definition of the measure and its views. The defaults must be already populated using {@link MetricDefinitionSettings#getCopyWithDefaultsPopulated(String)}!
     *
     * @see #addOrUpdateAndCacheInstrumentsWithViews(String, MetricDefinitionSettings, Map, Map)
     */
    public void addOrUpdateAndCacheMeasureWithViews(String measureName, MetricDefinitionSettings definition) {
        val registeredViews = viewManager.getAllExportedViews()
                .stream()
                .collect(Collectors.toMap(v -> v.getName().asString(), v -> v));
        val registeredMeasures = registeredViews.values()
                .stream()
                .map(View::getMeasure)
                .distinct()
                .collect(Collectors.toMap(Measure::getName, m -> m));

        addOrUpdateAndCacheMeasureWithViews(measureName, definition, registeredMeasures, registeredViews);

        // OTEL
        Map<String, io.opentelemetry.sdk.metrics.View> registeredOtelViews = openTelemetryController.getRegisteredViews()
                .values()
                .stream()
                .collect(Collectors.toMap(io.opentelemetry.sdk.metrics.View::getName, view -> view));

        Map<String, InstrumentSelector> registeredInstrumentSelectors = new HashMap<>();
        openTelemetryController.getRegisteredViews()
                .keySet()
                .stream()
                .distinct()
                .forEach(instrumentSelector -> registeredInstrumentSelectors.put(instrumentSelector.getInstrumentName(), instrumentSelector));

        addOrUpdateAndCacheInstrumentsWithViews(measureName, definition, registeredInstrumentSelectors, registeredOtelViews);
    }

    /**
     * Tries to create a measure based on the given definition as well as its views.
     * If the measure or a view already exists, info messages are printed out
     *
     * @param measureName        the name of the measure
     * @param definition         the definition of the measure and its views. The defaults
     *                           must be already populated using {@link MetricDefinitionSettings#getCopyWithDefaultsPopulated(String)}!
     * @param registeredMeasures a map of which measures are already registered at the OpenCensus API. Maps the names to the measures. Used to detect if the measure to create already exists.
     * @param registeredViews    same as registeredMeasures, but maps the view names to the corresponding registered views.
     */
    @VisibleForTesting
    void addOrUpdateAndCacheMeasureWithViews(String measureName, MetricDefinitionSettings definition, Map<String, Measure> registeredMeasures, Map<String, View> registeredViews) {
        try {
            Measure measure = registeredMeasures.get(measureName);
            if (measure != null) {
                updateMeasure(measureName, measure, definition);
            } else {
                measure = createNewMeasure(measureName, definition);
            }
            val resultMeasure = measure;

            InstrumentSelector selector = InstrumentSelector.builder().setName(measureName).build();

            val metricViews = definition.getViews();
            metricViews.forEach((name, view) -> {
                if (view.isEnabled()) {
                    try {
                        addAndRegisterOrUpdateView(name, resultMeasure, view, registeredViews);
                    } catch (Exception e) {
                        log.error("Error creating view '{}'!", name, e);
                    }
                }
            });

            //TODO: delete views which where created by this class but have been removed from the given metric as soon as OpenCensus supports it
            currentMetricDefinitionSettings.put(measureName, definition);
            cachedMeasures.put(measureName, measure);

        } catch (Exception e) {
            log.error("Error creating metric", e);
        }
    }

    /**
     * Tries to create an instrument based on the given definition as well as its views.
     * If the measure or a view already exists, info messages are printed out
     *
     * @param instrumentName                the name of the instrument
     * @param definition                    the definition of the instrument and its views. The defaults
     *                                      must be already populated using {@link MetricDefinitionSettings#getCopyWithDefaultsPopulated(String)}!
     * @param registeredInstrumentSelectors a map of which {@link InstrumentSelector} are already registered at the OpenTelemetry API via {@link OpenTelemetryControllerImpl#registeredViews). Maps the names to the instrument selectors. Used to detect if the instrument to create already exists.
     * @param registeredViews               same as registeredMeasures, but maps the view names to the corresponding registered views.
     */
    @VisibleForTesting
    void addOrUpdateAndCacheInstrumentsWithViews(String instrumentName, MetricDefinitionSettings definition, Map<String, InstrumentSelector> registeredInstrumentSelectors, Map<String, io.opentelemetry.sdk.metrics.View> registeredViews) {
        try {
            // update views first, as they need to be registered before we can create new instruments.
            Map<String, ViewDefinitionSettings> metricViews = definition.getViews();

            // get the instrument selector or create a new one if not yet registered.
            InstrumentSelector instrumentSelector = registeredInstrumentSelectors.containsKey(instrumentName) ? registeredInstrumentSelectors.get(instrumentName) : InstrumentSelector.builder()
                    .setName(instrumentName)
                    .build();

            metricViews.forEach((name, view) -> {
                if (view.isEnabled()) {
                    try {
                        addAndRegisterOrUpdateView(name, instrumentSelector, view, instrumentName, definition, registeredViews);
                    } catch (Exception e) {
                        log.error("Error creating view '{}'!", name, e);
                    }
                }
            });

            currentMetricDefinitionSettings.put(instrumentName, definition);

            openTelemetryConfiguredActionsQueue.offer(() -> {
                try {
                    // update or create the new instrument
                    // we need to create the instruments asynchronously as the OpenTelemetryController creates the new MeterProvider
                    // once all other classes finished their callbacks to the InspectitConfigChangedEvent.
                    Object instrument;
                    // TODO: do we allow updating an instrument?
                    if (instrumentSelector != null) {
                        instrument = cachedInstruments.get(instrumentName).getKey();
                        updateInstrument(instrumentName, cachedInstruments.get(instrumentName).getValue(), definition);
                    } else {
                        instrument = createNewInstrument(instrumentName, definition);
                    }
                    Object resultInstrument = instrument;
                    cachedInstruments.put(instrumentName, new AbstractMap.SimpleEntry(resultInstrument, OpenTelemetryUtils.getInstrumentDescriptor(resultInstrument)));
                } catch (Exception e) {
                    log.error("Error creating instrument", e);
                }
            });
        } catch (Exception e) {
            log.error("Error creating metric", e);
        }
    }

    /**
     * Queue of actions that will be executed the next time that {@link OpenTelemetryControllerImpl#configureOpenTelemetry()} has finished.
     */
    private Queue<Runnable> openTelemetryConfiguredActionsQueue = new LinkedList<>();

    @EventListener(OpenTelemetryConfiguredEvent.class)
    private void runActionsAfterOpenTelemetryConfigured() {
        log.info("OpenTelemetry has been configured. Running " + openTelemetryConfiguredActionsQueue.size() + " actions");
        while (!openTelemetryConfiguredActionsQueue.isEmpty()) {
            openTelemetryConfiguredActionsQueue.poll().run();
        }
    }

    private Measure createNewMeasure(String measureName, MetricDefinitionSettings fullDefinition) {
        switch (fullDefinition.getType()) {
            case LONG:
                return Measure.MeasureLong.create(measureName, fullDefinition.getDescription(), fullDefinition.getUnit());
            case DOUBLE:
                return Measure.MeasureDouble.create(measureName, fullDefinition.getDescription(), fullDefinition.getUnit());
            default:
                throw new RuntimeException("Unhandled measure type: " + fullDefinition.getType());
        }
    }

    /**
     * Creates a new {@link io.opentelemetry.sdk.metrics.AbstractInstrument} with the given definition.
     *
     * @param instrumentName the name of the instrument
     * @param fullDefinition the definitions used to create the instrument
     *
     * @return
     */
    private Object createNewInstrument(String instrumentName, MetricDefinitionSettings fullDefinition) {
        switch (fullDefinition.getType()) {
            case LONG:
                return OpenTelemetryUtils.getMeter().histogramBuilder(instrumentName).ofLongs().build();
            case DOUBLE:
                return OpenTelemetryUtils.getMeter().histogramBuilder(instrumentName).build();
            default:
                throw new RuntimeException(String.format("Unhandled instrument type: ", fullDefinition.getType()));
        }
    }

    private void updateMeasure(String measureName, Measure measure, MetricDefinitionSettings fullDefinition) {
        if (!fullDefinition.getDescription().equals(measure.getDescription())) {
            log.warn("Cannot update description of measure '{}' because it has been already registered in OpenCensus!", measureName);
        }
        if (!fullDefinition.getUnit().equals(measure.getUnit())) {
            log.warn("Cannot update unit of measure '{}' because it has been already registered in OpenCensus!", measureName);
        }
        if ((measure instanceof Measure.MeasureLong && fullDefinition.getType() != MetricDefinitionSettings.MeasureType.LONG) || (measure instanceof Measure.MeasureDouble && fullDefinition.getType() != MetricDefinitionSettings.MeasureType.DOUBLE)) {
            log.warn("Cannot update type of measure '{}' because it has been already registered in OpenCensus!", measureName);
        }
    }

    private void updateInstrument(String instrumentName, InstrumentDescriptor descriptor, MetricDefinitionSettings fullDefinition) {
        if (!fullDefinition.getDescription().equals(descriptor.getDescription())) {
            log.warn("Cannot update description of instrument '{}' because it has been already registered in OpenTelemetry!", instrumentName);
        }
        if (!fullDefinition.getUnit().equals(descriptor.getUnit())) {
            log.warn("Cannot update unit of instrument '{}' because it has been already registered in OpenTelemetry!", instrumentName);
        }
        if (fullDefinition.getType().toOpenTelemetryInstrumentValueType() != descriptor.getValueType()) {
            log.warn("Cannot update type of instrument '{}' because it has been already registered in OpenTelemetry!", instrumentName);
        }
    }

    /**
     * Creates a view if it does not exist yet.
     * Otherwise, prints info messages indicating that updating the view is not possible.
     *
     * @param viewName        the name of the view
     * @param measure         the measure which is used for the view
     * @param def             the definition of the view, on which
     *                        {@link ViewDefinitionSettings#getCopyWithDefaultsPopulated(String, String, String)} was already called.
     * @param registeredViews a map of which views are already registered at the OpenCensus API. Maps the view names to the views.
     */
    private void addAndRegisterOrUpdateView(String viewName, Measure measure, ViewDefinitionSettings def, Map<String, View> registeredViews) {
        View view = registeredViews.get(viewName);
        if (view != null) {
            updateOpenCensusView(viewName, def, view);
        } else {
            if (percentileViewManager.isViewRegistered(measure.getName(), viewName) || def.getAggregation() == ViewDefinitionSettings.Aggregation.QUANTILES) {
                addOrUpdatePercentileView(measure, viewName, def);
            } else {
                registerNewView(viewName, measure, def);
            }
        }
    }

    /**
     * Creates a view if it does not exist yet.
     * Otherwise, prints info messages indicating that updating the view is not possible.
     *
     * @param viewName           the name of the view
     * @param instrumentSelector the {@link InstrumentSelector} of the view
     * @param viewDef            the definition of the view, on which
     *                           {@link ViewDefinitionSettings#getCopyWithDefaultsPopulated(String, String, String)} was already called.
     * @param instrumentName     the name of the instrument corresponding to the view
     * @param metricDef          the definition of the metric corresponding to the view
     * @param registeredViews    a map of which views are already registered at the OpenTelemetry API. Maps the view names to the views.
     */
    private void addAndRegisterOrUpdateView(String viewName, InstrumentSelector instrumentSelector, ViewDefinitionSettings viewDef, String instrumentName, MetricDefinitionSettings metricDef, Map<String, io.opentelemetry.sdk.metrics.View> registeredViews) {
        io.opentelemetry.sdk.metrics.View view = registeredViews.get(viewName);
        if (view != null) {
            updateOpenTelemetryView(viewName, viewDef, instrumentSelector, view);
        } else {
            // TODO: do we need to have the percentile view manager?
            /*if (percentileViewManager.isViewRegistered(instrumentSelector.getInstrumentName(), viewName) || viewDef.getAggregation() == ViewDefinitionSettings.Aggregation.QUANTILES) {
                addOrUpdatePercentileView(instrumentName, metricDef, viewName, viewDef);
            } else {
                registerNewView(viewName, viewDef, instrumentSelector);
            }*/
            registerNewView(viewName, viewDef, instrumentSelector);
        }
    }

    /**
     * Updates the {@link io.opentelemetry.sdk.metrics.View} registered at the {@link OpenTelemetryControllerImpl}.
     * For this, the previews {@code View} gets unregistered, a new {@code View} is built and {@link OpenTelemetryControllerImpl#registerView(InstrumentSelector, io.opentelemetry.sdk.metrics.View)}  registered}
     *
     * @param viewName
     * @param def
     * @param instrumentSelector
     * @param view
     */
    // TODO: do we allow updating of Views in general? Theoretically, individual fields can also be updated in the previous View using reflection (e.g., the AttributesFilter etc.)
    private void updateOpenTelemetryView(String viewName, ViewDefinitionSettings def, InstrumentSelector instrumentSelector, io.opentelemetry.sdk.metrics.View view) {
        // TODO: do we allow changing aggregation, description etc. of the view or not? see updateOpenCensusView for reference

        openTelemetryController.unregisterView(instrumentSelector);
        // TODO: do we need to rebuild the view or can we use the previous view? I.e., does the View gets "destroyed" when we rebuild the SdkMeterProvider in the OpenTelmetryControllerImpl?
        registerNewView(viewName, def, instrumentSelector);
    }

    private void addOrUpdatePercentileView(Measure measure, String viewName, ViewDefinitionSettings def) {
        if (def.getAggregation() != ViewDefinitionSettings.Aggregation.QUANTILES) {
            log.warn("Cannot switch aggregation type for View '{}' from QUANTILES to {}", viewName, def.getAggregation());
            return;
        }
        Set<TagKey> viewTags = getTagKeysForView(def);
        Set<String> tagsAsStrings = viewTags.stream().map(TagKey::getName).collect(Collectors.toSet());
        boolean minEnabled = def.getQuantiles().contains(0.0);
        boolean maxEnabled = def.getQuantiles().contains(1.0);
        List<Double> percentilesFiltered = def.getQuantiles()
                .stream()
                .filter(p -> p > 0 && p < 1)
                .collect(Collectors.toList());
        percentileViewManager.createOrUpdateView(measure.getName(), viewName, measure.getUnit(), def.getDescription(), minEnabled, maxEnabled, percentilesFiltered, def.getTimeWindow()
                .toMillis(), tagsAsStrings, def.getMaxBufferedPoints());
    }

    private void addOrUpdatePercentileView(String measureName, MetricDefinitionSettings metricDef, String viewName, ViewDefinitionSettings viewDef) {
        // TODO: fix
        if (true) {
            throw new RuntimeException("not yet implemented");
        }

        if (viewDef.getAggregation() != ViewDefinitionSettings.Aggregation.QUANTILES) {
            log.warn("Cannot switch aggregation type for View '{}' from QUANTILES to {}", viewName, viewDef.getAggregation());
            return;
        }
        // TODO: change to OTEL attributes
        Set<TagKey> viewTags = getTagKeysForView(viewDef);
        Set<String> tagsAsStrings = viewTags.stream().map(TagKey::getName).collect(Collectors.toSet());
        boolean minEnabled = viewDef.getQuantiles().contains(0.0);
        boolean maxEnabled = viewDef.getQuantiles().contains(1.0);
        List<Double> percentilesFiltered = viewDef.getQuantiles()
                .stream()
                .filter(p -> p > 0 && p < 1)
                .collect(Collectors.toList());
        percentileViewManager.createOrUpdateView(measureName, viewName, metricDef.getUnit(), viewDef.getDescription(), minEnabled, maxEnabled, percentilesFiltered, viewDef.getTimeWindow()
                .toMillis(), tagsAsStrings, viewDef.getMaxBufferedPoints());
    }

    private void registerNewView(String viewName, Measure measure, ViewDefinitionSettings def) {
        Set<TagKey> viewTags = getTagKeysForView(def);
        View view = View.create(View.Name.create(viewName), def.getDescription(), measure, createAggregation(def), new ArrayList<>(viewTags));
        viewManager.registerView(view);
    }

    /**
     * Registers a new {@link io.opentelemetry.sdk.metrics.View} with the given {@code name}, {@code definitions}, and {@code selector}
     *
     * @param name
     * @param def
     * @param selector
     */
    private void registerNewView(String name, ViewDefinitionSettings def, InstrumentSelector selector) {

        Set<AttributeKey> allowedAttributeKeys = getAttributeKeysForView(def);
        io.opentelemetry.sdk.metrics.View view = io.opentelemetry.sdk.metrics.View.builder()
                .setDescription(def.getDescription())
                .setAggregation(def.getAggregationAsOpenTelemetryAggregation())
                .setName(name)
                .setAttributeFilter(s -> allowedAttributeKeys.contains(s))
                .build();

        openTelemetryController.registerView(selector, view);
    }

    private void updateOpenCensusView(String viewName, ViewDefinitionSettings def, View view) {
        if (!def.getDescription().equals(view.getDescription())) {
            log.warn("Cannot update description of view '{}' because it has been already registered in OpenCensus!", viewName);
        }
        if (!isAggregationEqual(view.getAggregation(), def)) {
            log.warn("Cannot update aggregation of view '{}' because it has been already registered in OpenCensus!", viewName);
        }
        Set<TagKey> presentTagKeys = new HashSet<>(view.getColumns());

        Set<TagKey> viewTags = getTagKeysForView(def);
        presentTagKeys.stream()
                .filter(t -> !viewTags.contains(t))
                .forEach(tag -> log.warn("Cannot remove tag '{}' from view '{}' because it has been already registered in OpenCensus!", tag.getName(), viewName));
        viewTags.stream()
                .filter(t -> !presentTagKeys.contains(t))
                .forEach(tag -> log.warn("Cannot add tag '{}' to view '{}' because it has been already registered in OpenCensus!", tag.getName(), viewName));
    }

    /**
     * Collects the tags which should be used for the given view.
     * This function includes the common tags if requested,
     * then applies the {@link ViewDefinitionSettings#getTags()}
     * and finally converts them to {@link TagKey}s.
     *
     * @param def the view definition for which the tags should be collected.
     *
     * @return the set of tag keys
     */
    private Set<TagKey> getTagKeysForView(ViewDefinitionSettings def) {
        Set<TagKey> viewTags = new HashSet<>();
        if (def.isWithCommonTags()) {
            commonTags.getCommonTagKeys()
                    .stream()
                    .filter(t -> def.getTags().get(t.getName()) != Boolean.FALSE)
                    .forEach(viewTags::add);
        }
        def.getTags()
                .entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(TagKey::create)
                .forEach(viewTags::add);
        return viewTags;
    }

    /**
     * Collects the attributes which should be used for the given view.
     * This function includes the common tags if requested,
     * then applies the {@link ViewDefinitionSettings#getTags()}
     * and finally converts them to {@link io.opentelemetry.api.common.AttributeKey}s.
     *
     * @param def the view definition for which the attributes should be collected.
     *
     * @return the set of tag keys
     */
    private Set<AttributeKey> getAttributeKeysForView(ViewDefinitionSettings def) {
        Set<AttributeKey> viewTags = new HashSet<>();

        // first add enabled common attributes
        if (def.isWithCommonTags()) {
            commonTags.getCommonAttributeKeys()
                    .stream()
                    .filter(t -> def.getTags().get(t.getKey()) != Boolean.FALSE)
                    .forEach(viewTags::add);
        }
        // then attributes specified for the view
        def.getTags()
                .entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(entry -> AttributeKey.stringKey(entry.getKey()))
                .forEach(viewTags::add);
        return viewTags;
    }

    private boolean isAggregationEqual(Aggregation instance, ViewDefinitionSettings view) {
        switch (view.getAggregation()) {
            case COUNT:
                return instance instanceof Aggregation.Count;
            case SUM:
                return instance instanceof Aggregation.Sum;
            case HISTOGRAM:
                return instance instanceof Aggregation.Distribution && ((Aggregation.Distribution) instance).getBucketBoundaries()
                        .equals(view.getBucketBoundaries());
            case LAST_VALUE:
                return instance instanceof Aggregation.LastValue;
            default:
                throw new RuntimeException("Unhandled aggregation type: " + view.getAggregation());
        }
    }

    private Aggregation createAggregation(ViewDefinitionSettings view) {
        switch (view.getAggregation()) {
            case COUNT:
                return Aggregation.Count.create();
            case SUM:
                return Aggregation.Sum.create();
            case HISTOGRAM:
                return Aggregation.Distribution.create(BucketBoundaries.create(view.getBucketBoundaries()));
            case LAST_VALUE:
                return Aggregation.LastValue.create();
            default:
                throw new RuntimeException("Unhandled aggregation type: " + view.getAggregation());
        }
    }
}

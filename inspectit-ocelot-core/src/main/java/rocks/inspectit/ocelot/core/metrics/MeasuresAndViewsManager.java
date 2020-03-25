package rocks.inspectit.ocelot.core.metrics;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
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
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

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
    private CommonTagsManager commonTags;

    @Autowired
    private InspectitEnvironment env;

    /**
     * Caches all created measures.
     */
    private final ConcurrentHashMap<String, Measure> cachedMeasures = new ConcurrentHashMap<>();

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
     * Calls {@link #getMeasureDouble(String)} and records a measurement if the measure was found.
     *
     * @param measureName the name of the measure
     * @param resultMap   the map to store the measurement value in
     * @param value       the measurement value for this measure
     * @return true, if the measure exists, has type double and the measurement was recorded, false otherwise
     */
    public boolean tryRecordingMeasurement(String measureName, MeasureMap resultMap, double value) {
        val measure = getMeasureDouble(measureName);
        if (measure.isPresent()) {
            resultMap.put(measure.get(), value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Records a measurement for the given measure, if it exists.
     * Depending on the measure type either {@link Number#doubleValue()}
     * or {@link Number#longValue()} is used.
     *
     * @param measureName the name of the measure
     * @param resultMap   the map to store the measurement value in
     * @param value       the measurement value for this measure
     * @return true, if the measure exists, false otherwise
     */
    public boolean tryRecordingMeasurement(String measureName, MeasureMap resultMap, Number value) {
        val measure = getMeasure(measureName);
        if (measure.isPresent()) {
            val m = measure.get();
            if (m instanceof Measure.MeasureLong) {
                resultMap.put((Measure.MeasureLong) m, value.longValue());
            } else if (m instanceof Measure.MeasureDouble) {
                resultMap.put((Measure.MeasureDouble) m, value.doubleValue());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calls {@link #getMeasureLong(String)} and records a measurement if the measure was found.
     *
     * @param measureName the name of the measure
     * @param resultMap   the map to store the measurement value in
     * @param value       the measurement value for this measure
     * @return true, if the measure exists, has type long and the measurement was recorded, false otherwise
     */
    public boolean tryRecordingMeasurement(String measureName, MeasureMap resultMap, long value) {
        val measure = getMeasureLong(measureName);
        if (measure.isPresent()) {
            resultMap.put(measure.get(), value);
            return true;
        } else {
            return false;
        }
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
                val defWithDefaults = def.getCopyWithDefaultsPopulated(name);
                val oldDef = currentMetricDefinitionSettings.get(name);
                if (defWithDefaults.isEnabled() && !defWithDefaults.equals(oldDef)) {
                    addOrUpdateAndCacheMeasureWithViews(name, defWithDefaults);
                }
            });
        }
        //TODO: delete removed measures and views as soon as this is possible in Open-Census
    }

    /**
     * Tries to create a measure based on on the given definition, with checking measures and views reported by {@link #viewManager}.
     * <p>
     * If the measure or a view already exists, info messages are printed out
     *
     * @param measureName the name of the measure
     * @param definition  the definition of the measure and its views. The defaults must be already populated using {@link MetricDefinitionSettings#getCopyWithDefaultsPopulated(String)}!
     * @see #addOrUpdateAndCacheMeasureWithViews(String, MetricDefinitionSettings, Map, Map)
     */
    public void addOrUpdateAndCacheMeasureWithViews(String measureName, MetricDefinitionSettings definition) {
        val registeredViews = viewManager.getAllExportedViews()
                .stream()
                .collect(Collectors.toMap(v -> v.getName().asString(), v -> v));
        val registeredMeasures = registeredViews.values().stream()
                .map(View::getMeasure)
                .distinct()
                .collect(Collectors.toMap(Measure::getName, m -> m));

        this.addOrUpdateAndCacheMeasureWithViews(measureName, definition, registeredMeasures, registeredViews);
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

    private Measure createNewMeasure(String measureName, MetricDefinitionSettings fullDefinition) {
        Measure measure;
        switch (fullDefinition.getType()) {
            case LONG:
                measure = Measure.MeasureLong.create(measureName, fullDefinition.getDescription(), fullDefinition.getUnit());
                break;
            case DOUBLE:
                measure = Measure.MeasureDouble.create(measureName, fullDefinition.getDescription(), fullDefinition.getUnit());
                break;
            default:
                throw new RuntimeException("Unhandled measure type: " + fullDefinition.getType());
        }
        return measure;
    }

    private void updateMeasure(String measureName, Measure measure, MetricDefinitionSettings fullDefinition) {
        if (!fullDefinition.getDescription().equals(measure.getDescription())) {
            log.info("Cannot update description of measure '{}' because it has been already registered in OpenCensus!", measureName);
        }
        if (!fullDefinition.getUnit().equals(measure.getUnit())) {
            log.info("Cannot update unit of measure '{}' because it has been already registered in OpenCensus!", measureName);
        }
        if ((measure instanceof Measure.MeasureLong && fullDefinition.getType() != MetricDefinitionSettings.MeasureType.LONG)
                || (measure instanceof Measure.MeasureDouble && fullDefinition.getType() != MetricDefinitionSettings.MeasureType.DOUBLE)) {
            log.info("Cannot update type of measure '{}' because it has been already registered in OpenCensus!", measureName);
        }
    }

    /**
     * Creates a view if does not exist yet.
     * Otherwise prints info messages indicating that updating the view is not possible.
     *
     * @param viewName        the name of the view
     * @param measure         the measure which is used for the view
     * @param def             the definition of the view, on which
     *                        {@link ViewDefinitionSettings#getCopyWithDefaultsPopulated(String, String, String)} was already called.
     * @param registeredViews a map of which views are already registered at the OpenCensus API. Maps the view names to the views.
     */
    private void addAndRegisterOrUpdateView(String viewName, Measure measure, ViewDefinitionSettings def, Map<String, View> registeredViews) {
        Set<TagKey> viewTags = getTagKeysForView(def);

        View view = registeredViews.get(viewName);
        if (view != null) {
            updateView(viewName, def, viewTags, view);
        } else {
            registerNewView(viewName, measure, def, viewTags);
        }
    }

    private void registerNewView(String viewName, Measure measure, ViewDefinitionSettings def, Set<TagKey> viewTags) {
        View view;
        view = View.create(
                View.Name.create(viewName),
                def.getDescription(),
                measure,
                createAggregation(def),
                new ArrayList<>(viewTags));
        viewManager.registerView(view);
    }

    private void updateView(String viewName, ViewDefinitionSettings def, Set<TagKey> viewTags, View view) {
        if (!def.getDescription().equals(view.getDescription())) {
            log.info("Cannot update description of view '{}' because it has been already registered in OpenCensus!", viewName);
        }
        if (!isAggregationEqual(view.getAggregation(), def)) {
            log.info("Cannot update aggregation of view '{}' because it has been already registered in OpenCensus!", viewName);
        }
        Set<TagKey> presentTagKeys = new HashSet<>(view.getColumns());

        presentTagKeys.stream()
                .filter(t -> !viewTags.contains(t))
                .forEach(tag -> log.info("Cannot remove tag '{}' from view '{}' because it has been already registered in OpenCensus!", tag.getName(), viewName));
        viewTags.stream()
                .filter(t -> !presentTagKeys.contains(t))
                .forEach(tag -> log.info("Cannot add tag '{}' to view '{}' because it has been already registered in OpenCensus!", tag.getName(), viewName));
    }

    /**
     * Collects the tags which should be used for the given view.
     * This function includes the common tags if requested,
     * then applies the {@link ViewDefinitionSettings#getTags()}
     * and finally converts them to {@link TagKey}s.
     *
     * @param def the view definition for which the tags should be collected.
     * @return the set of tag keys
     */
    private Set<TagKey> getTagKeysForView(ViewDefinitionSettings def) {
        Set<TagKey> viewTags = new HashSet<>();
        if (def.isWithCommonTags()) {
            commonTags.getCommonTagKeys().stream()
                    .filter(t -> def.getTags().get(t.getName()) != Boolean.FALSE)
                    .forEach(viewTags::add);
        }
        def.getTags().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .map(TagKey::create)
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
                return instance instanceof Aggregation.Distribution &&
                        ((Aggregation.Distribution) instance).getBucketBoundaries().equals(view.getBucketBoundaries());
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

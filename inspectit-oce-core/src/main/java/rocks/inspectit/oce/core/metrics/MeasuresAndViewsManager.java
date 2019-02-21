package rocks.inspectit.oce.core.metrics;

import com.google.common.annotations.VisibleForTesting;
import io.opencensus.stats.*;
import io.opencensus.tags.TagKey;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.config.InspectitConfigChangedEvent;
import rocks.inspectit.oce.core.config.InspectitEnvironment;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;
import rocks.inspectit.oce.core.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.oce.core.config.model.metrics.definition.ViewDefinitionSettings;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

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
    private final Map<String, MetricDefinitionSettings> definitionsUsedToDefineMetrics = new HashMap<>();

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
     * Creates the measures and views defined via {@link MetricsSettings#getDefinitions()}.
     * OpenCensus does currently not allow the removal of views, therefore updating metrics is not possible.
     */
    @EventListener(InspectitConfigChangedEvent.class)
    @Order(CommonTagsManager.CONFIG_EVENT_LISTENER_ORDER + 1) //to ensure common tags are updated first
    @PostConstruct
    public void updateMetricDefinitions() {
        MetricsSettings metricsSettings = env.getCurrentConfig().getMetrics();
        if (metricsSettings.isEnabled()) {
            val newMetricDefinitions = metricsSettings.getDefinitions();

            val registeredViews = viewManager.getAllExportedViews()
                    .stream()
                    .collect(Collectors.toMap(v -> v.getName().asString(), v -> v));
            val registeredMeasures = registeredViews.values().stream()
                    .map(View::getMeasure)
                    .distinct()
                    .collect(Collectors.toMap(Measure::getName, m -> m));

            newMetricDefinitions.forEach((name, def) -> {
                val defWithDefaults = def.getCopyWithDefaultsPopulated(name);
                val oldDef = definitionsUsedToDefineMetrics.get(defWithDefaults.getName());
                if (defWithDefaults.isEnabled() && !defWithDefaults.equals(oldDef)) {
                    addOrUpdateAndCacheMeasureWithViews(defWithDefaults, registeredMeasures, registeredViews);
                }
            });
        }
        //TODO: delete removed measures and views as soon as this is possible in Open-Census
    }

    /**
     * Tries to create a measure based on the given definition as well as its views.
     * If the measure or a view already exists, info messages are printed out
     *
     * @param definition         the definition of the measure and its views. The defaults
     *                           must be already populated using {@link MetricDefinitionSettings#getCopyWithDefaultsPopulated(String)}!
     * @param registeredMeasures a map of which measures are already registered at the OpenCensus API. Maps the names to the measures. Used to detect if the measure to create already exists.
     * @param registeredViews    same as registeredMeasures, but maps the view names to the corresponding registered views.
     */
    @VisibleForTesting
    void addOrUpdateAndCacheMeasureWithViews(MetricDefinitionSettings definition, Map<String, Measure> registeredMeasures, Map<String, View> registeredViews) {
        try {
            String measureName = definition.getName();
            Measure measure = registeredMeasures.get(measureName);
            if (measure != null) {
                updateMeasure(measure, definition);
            } else {
                measure = createNewMeasure(definition);
            }
            val resultMeasure = measure;

            val metricViews = definition.getViews();
            metricViews.forEach((name, view) -> {
                if (view.isEnabled()) {
                    try {
                        addAndRegisterOrUpdateView(resultMeasure, view, registeredViews);
                    } catch (Exception e) {
                        log.error("Error creating view '{}'!", name, e);
                    }
                }
            });

            //TODO: delete views which where created by this class but have been removed from the given metric as soon as OpenCensus supports it
            definitionsUsedToDefineMetrics.put(measureName, definition);
            cachedMeasures.put(measureName, measure);

        } catch (Exception e) {
            log.error("Error creating metric", e);
        }
    }

    private Measure createNewMeasure(MetricDefinitionSettings fullDefinition) {
        Measure measure;
        switch (fullDefinition.getType()) {
            case LONG:
                measure = Measure.MeasureLong.create(fullDefinition.getName(), fullDefinition.getDescription(), fullDefinition.getUnit());
                break;
            case DOUBLE:
                measure = Measure.MeasureDouble.create(fullDefinition.getName(), fullDefinition.getDescription(), fullDefinition.getUnit());
                break;
            default:
                throw new RuntimeException("Unhandled case!");
        }
        return measure;
    }

    private void updateMeasure(Measure measure, MetricDefinitionSettings fullDefinition) {
        if (!fullDefinition.getDescription().equals(measure.getDescription())) {
            log.info("Cannot update description of measure '{}' because it has been already registered in OpenCensus!", fullDefinition.getName());
        }
        if (!fullDefinition.getUnit().equals(measure.getUnit())) {
            log.info("Cannot update unit of measure '{}' because it has been already registered in OpenCensus!", fullDefinition.getName());
        }
        if ((measure instanceof Measure.MeasureLong && fullDefinition.getType() != MetricDefinitionSettings.MeasureType.LONG)
                || (measure instanceof Measure.MeasureDouble && fullDefinition.getType() != MetricDefinitionSettings.MeasureType.DOUBLE)) {
            log.info("Cannot update type of measure '{}' because it has been already registered in OpenCensus!", fullDefinition.getName());
        }
    }

    /**
     * Creates a view if does not exist yet.
     * Otherwise prints info messages indicating that updating the view is not possible.
     *
     * @param measure         the measure which is used for the view
     * @param def             the definition of the view, on which
     *                        {@link ViewDefinitionSettings#getCopyWithDefaultsPopulated(String, String, String)} was already called.
     * @param registeredViews a map of which views are already registered at the OpenCensus API. Maps the view names to the views.
     */
    private void addAndRegisterOrUpdateView(Measure measure, ViewDefinitionSettings def, Map<String, View> registeredViews) {
        Set<TagKey> viewTags = getTagKeysForView(def);

        View view = registeredViews.get(def.getName());
        if (view != null) {
            updateView(def, viewTags, view);
        } else {
            registerNewView(measure, def, viewTags);
        }
    }

    private void registerNewView(Measure measure, ViewDefinitionSettings def, Set<TagKey> viewTags) {
        View view;
        view = View.create(
                View.Name.create(def.getName()),
                def.getDescription(),
                measure,
                getAggregationOfType(def.getAggregation(), def.getBucketBoundaries()),
                new ArrayList<>(viewTags));
        viewManager.registerView(view);
    }

    private void updateView(ViewDefinitionSettings def, Set<TagKey> viewTags, View view) {
        if (!def.getDescription().equals(view.getDescription())) {
            log.info("Cannot update description of view '{}' because it has been already registered in OpenCensus!", def.getName());
        }
        if (!isAggregationOfType(def.getAggregation(), view.getAggregation())) {
            log.info("Cannot update aggregation of view '{}' because it has been already registered in OpenCensus!", def.getName());
        }
        Set<TagKey> presentTagKeys = new HashSet<>(view.getColumns());

        presentTagKeys.stream()
                .filter(t -> !viewTags.contains(t))
                .forEach(tag -> log.info("Cannot remove tag '{}' from view '{}' because it has been already registered in OpenCensus!", tag.getName(), def.getName()));
        viewTags.stream()
                .filter(t -> !presentTagKeys.contains(t))
                .forEach(tag -> log.info("Cannot add tag '{}' to view '{}' because it has been already registered in OpenCensus!", tag.getName(), def.getName()));
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

    private boolean isAggregationOfType(ViewDefinitionSettings.Aggregation aggregationType, Aggregation instance) {
        switch (aggregationType) {
            case COUNT:
                return instance instanceof Aggregation.Count;
            case SUM:
                return instance instanceof Aggregation.Sum;
            case HISTOGRAM:
                return instance instanceof Aggregation.Distribution;
            case LAST_VALUE:
                return instance instanceof Aggregation.LastValue;
            default:
                throw new RuntimeException("Unhandled case!");
        }
    }


    private Aggregation getAggregationOfType(ViewDefinitionSettings.Aggregation aggregationType, List<Double> histogramBuckets) {
        switch (aggregationType) {
            case COUNT:
                return Aggregation.Count.create();
            case SUM:
                return Aggregation.Sum.create();
            case HISTOGRAM:
                return Aggregation.Distribution.create(BucketBoundaries.create(histogramBuckets));
            case LAST_VALUE:
                return Aggregation.LastValue.create();
            default:
                throw new RuntimeException("Unhandled case!");
        }
    }
}

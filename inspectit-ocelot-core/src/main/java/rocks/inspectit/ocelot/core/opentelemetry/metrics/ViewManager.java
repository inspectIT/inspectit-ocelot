package rocks.inspectit.ocelot.core.opentelemetry.metrics;

import io.opentelemetry.sdk.metrics.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import rocks.inspectit.ocelot.config.model.metrics.definition.MetricDefinitionSettings;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.AggregationType;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.TimeWindowViewManager;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores all user-specified metric views.
 * Even if the user has not specified any metric view at all, OpenTelemetry will automatically register default views
 * for every created instrument. Those views will not be accessible from here.
 */
@Component
@Slf4j
public class ViewManager {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private TimeWindowViewManager timeWindowViewManager;

    @Autowired
    private CommonAttributesManager commonAttributes;

    /** Stores for each metric all of it's registered views */
    private final Map<String, Map<String, ViewDefinitionSettings>> currentViews = new ConcurrentHashMap<>();

    /** Cached OpenTelemetry views, which should be registered via {@link SdkMeterProviderBuilder} */
    private final Map<InstrumentSelector, View> cachedViews = new ConcurrentHashMap<>();

    /**
     * Current set of metrics, which should be removed.
     * Should be cleared and refilled for every update process.
     */
    private final Set<String> toBeRemovedMetrics = new HashSet<>();

    /**
     * Current set of views for metrics, which should be removed.
     * Should be cleared and refilled for every update process.
     */
    private final Map<String, Set<String>> toBeRemovedViews = new HashMap<>();

    /**
     * @return true, if the currently configured views do not match with the registered views
     */
    public boolean shouldUpdateViews() {
        Map<String, MetricDefinitionSettings> metrics = env.getCurrentConfig().getMetrics().getDefinitions();

        return metrics.entrySet().stream()
                .anyMatch(entry -> {
                    String metricName = entry.getKey();
                    MetricDefinitionSettings defWithDefaults = entry.getValue().getCopyWithDefaultsPopulated(metricName);

                    return shouldUpdateMetric(metricName, defWithDefaults);
                });
    }

    /**
     * Checks, if we need to update registered views for the provided metric.
     * If the metric is disabled or no views were specified, there shouldn't be any views registered here.
     *
     * @return true, if the registered views for the provided metric should be updated
     */
    private boolean shouldUpdateMetric(String metricName, MetricDefinitionSettings metricDefinition) {
        val views = metricDefinition.getViews();

        if (!metricDefinition.isEnabled() || CollectionUtils.isEmpty(views)) {
            return currentViews.containsKey(metricName);
        } else {
            return views.entrySet().stream()
                    .anyMatch(viewEntry -> {
                        String viewName = viewEntry.getKey();
                        ViewDefinitionSettings settings = viewEntry.getValue();

                        return shouldUpdateView(metricName, viewName, settings);
                    });
        }
    }

    /**
     * Checks, if we need to update the registered view.
     * If the view is disabled, there shouldn't be any view registered here.
     *
     * @return true, if the registered view should be updated
     */
    private boolean shouldUpdateView(String metricName, String viewName, ViewDefinitionSettings settings) {
        if (settings.isEnabled()) {
            return isNotRegistered(metricName, viewName, settings);
        } else {
            return currentViews.entrySet().stream()
                    .anyMatch(e -> e.getValue().containsKey(viewName));
        }
    }

    /**
     * @return true, if the current view has other settings than the provided one
     */
    private boolean isNotRegistered(String metricName, String viewName, ViewDefinitionSettings settings) {
        if (currentViews.containsKey(metricName)) {
            Map<String, ViewDefinitionSettings> views = currentViews.get(metricName);

            if (views.containsKey(viewName)) {
                ViewDefinitionSettings currentDefinition = views.get(viewName);
                return !currentDefinition.equals(settings);
            }
        }
        return true;
    }

    /**
     * Processes all active views for each metric and updates the {@link #currentViews} accordingly.
     * Views, which are no longer required will be removed from {@link #currentViews}.
     *
     * @return the views which should be registered for OpenTelemetry
     */
    public Map<InstrumentSelector, View> processViews(boolean shouldUpdate) {
        if (shouldUpdate) {
            resetCollections();
            val activeMetricViews = getActiveMetricViews();
            activeMetricViews.forEach(this::processMetric);
            removeViews();
        }
        return cachedViews;
    }

    /**
     * Processes all views for the provided metric.
     */
    private void processMetric(String metricName, Map<String, ViewDefinitionSettings> viewsMap) {
        toBeRemovedMetrics.remove(metricName);
        viewsMap.forEach((viewName, settings) -> processView(metricName, viewName, settings));
    }

    /**
     * Creates a new view or updates the existing one.
     */
    private void processView(String metricName, String viewName, ViewDefinitionSettings newSettings) {
        log.debug("Processing view: {}", viewName);
        if (toBeRemovedViews.containsKey(metricName))
            toBeRemovedViews.get(metricName).remove(viewName);

        val currentViewsForMetric = currentViews.computeIfAbsent(metricName, (name) -> new HashMap<>());
        boolean updated;

        if (currentViewsForMetric.containsKey(viewName)) {
            ViewDefinitionSettings currentSettings = currentViewsForMetric.get(viewName);
            updated = updateView(metricName, viewName, currentSettings, newSettings);
        } else {
            createView(metricName, viewName, newSettings);
            updated = true;
        }

        if (updated) {
            currentViewsForMetric.put(viewName, newSettings);
            log.debug("The view '{}' for metric '{}' was updated", viewName, metricName);
        }
    }

    /**
     * Creates a new view from the provided settings
     */
    private void createView(String metricName, String viewName, ViewDefinitionSettings settings) {
        if (settings.getAggregation().isTimeWindowAggregation()) {
            processTimeWindowView(metricName, viewName, settings);
        } else {
            View view = createOpenTelemetryView(viewName, settings);
            InstrumentSelector selector = createInstrumentSelector(metricName);
            cachedViews.put(selector, view);
        }
    }

    /**
     * Creates an updated view from the provided settings, if the settings have changed
     *
     * @return true, if the new settings differ from the current ones
     */
    private boolean updateView(String metricName, String viewName, ViewDefinitionSettings currentSettings, ViewDefinitionSettings newSettings) {
        if (!currentSettings.equals(newSettings)) {
           createView(metricName, viewName, newSettings);
           return true;
        }
        return false;
    }

    /**
     * Processes the view as custom time-window view, which is handled by {@link TimeWindowViewManager}
     * instead of OpenTelemetry.
     *
     * @param metricName the metric name
     * @param viewName the view name
     * @param settings the view settings
     */
    private void processTimeWindowView(String metricName, String viewName, ViewDefinitionSettings settings) {
        String unit = getUnit(metricName);
        timeWindowViewManager.createOrUpdateView(metricName, viewName, unit, settings);
    }

    /**
     * Creates a new view from the provided settings.
     *
     * @param viewName the view name
     * @param settings the view settings
     *
     * @return the created view
     */
    private View createOpenTelemetryView(String viewName, ViewDefinitionSettings settings) {
        Aggregation aggregation = convertAggregation(settings);
        ViewBuilder builder =  View.builder()
                .setName(viewName)
                .setDescription(settings.getDescription())
                .setAggregation(aggregation)
                .setCardinalityLimit(settings.getCardinalityLimit());
        boolean withCommonAttributes = settings.isWithCommonAttributes();

        if (!CollectionUtils.isEmpty(settings.getAttributes())) {
            builder.setAttributeFilter((attribute) -> filterAttribute(settings, attribute, withCommonAttributes));
        }
        else if (withCommonAttributes) {
            builder.setAttributeFilter(this::isCommonAttribute);
        }
        else {
            builder.setAttributeFilter((attr) -> false); // Reject all attributes
        }

        return builder.build();
    }

    /**
     * Creates a selector so the view can be applied to their particular metric.
     * At them moment, we select the metric solely by their name.
     *
     * @param metricName the metric name
     *
     * @return the instrument selector
     */
    private InstrumentSelector createInstrumentSelector(String metricName) {
        return InstrumentSelector.builder()
                .setName(metricName)
                .build();
    }

    /**
     * Checks, if the view includes the provided attribute key. Common attributes are included by default,
     * expect they are explicitly disabled for the view.
     *
     * @param settings              the view settings
     * @param attribute             the current attribute key
     * @param withCommonAttributes  whether we should include common attributes to the filter
     *
     * @return true, if this attribute key should be used for the provided view
     */
    private boolean filterAttribute(ViewDefinitionSettings settings, String attribute, boolean withCommonAttributes) {
        val viewAttributes = settings.getAttributes();

        if (withCommonAttributes && isCommonAttribute(attribute))
            return viewAttributes.getOrDefault(attribute, true);

        return viewAttributes.getOrDefault(attribute, false);
    }

    /**
     * @param attribute the current attribute key
     *
     * @return true, if the attribute key is defined globally
     */
    private boolean isCommonAttribute(String attribute) {
        return commonAttributes.getCommonAttributeKeys().contains(attribute);
    }

    /**
     * Removes all views, which are no longer required
     */
    private void removeViews() {
        // remove views for metrics
        toBeRemovedViews.forEach((metric, views) ->
                views.forEach(view -> {
                    if (currentViews.containsKey(metric))
                        currentViews.get(metric).remove(view);
                    timeWindowViewManager.removeView(metric, view);
                })
        );

        // remove metrics with all of their views
        toBeRemovedMetrics.forEach(metric -> {
            currentViews.remove(metric);
            timeWindowViewManager.removeViews(metric);
        });
    }

    /**
     * Resets removal collections and cached views.
     */
    private void resetCollections() {
        cachedViews.clear();
        toBeRemovedMetrics.clear();
        toBeRemovedViews.clear();

        currentViews.forEach((metricName, viewsMap) -> {
            toBeRemovedMetrics.add(metricName);
            toBeRemovedViews.put(metricName, new HashSet<>(viewsMap.keySet()));
        });
    }

    /**
     * @return the currently configured map of all metric names and their particular active views
     */
    private Map<String, Map<String, ViewDefinitionSettings>> getActiveMetricViews() {
        if(!env.getCurrentConfig().getMetrics().isEnabled()) return Collections.emptyMap();

        Map<String, Map<String, ViewDefinitionSettings>> metricViews = new HashMap<>();
        val metricDefinitions = env.getCurrentConfig().getMetrics().getDefinitions();

        metricDefinitions.entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .forEach((e) -> {
                    String metricName = e.getKey();
                    MetricDefinitionSettings defWithDefaults = e.getValue().getCopyWithDefaultsPopulated(metricName);

                    if (!CollectionUtils.isEmpty(defWithDefaults.getViews())) {
                        Map<String, ViewDefinitionSettings> activeViews = getActiveViews(defWithDefaults);
                        metricViews.put(metricName, activeViews);
                    }
                });

        return metricViews;
    }

    /**
     * @return the map of all active views for the provided metric
     */
    private Map<String, ViewDefinitionSettings> getActiveViews(MetricDefinitionSettings metricDefinition) {
        Map<String, ViewDefinitionSettings> activeViews = new HashMap<>();
        metricDefinition.getViews().entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .forEach(e -> activeViews.put(e.getKey(), e.getValue()));
        return activeViews;
    }

    /**
     * Converts the {@link AggregationType} to a proper OpenTelemetry {@link Aggregation}.
     *
     * @param settings the view settings
     *
     * @return the converted {@link Aggregation}
     */
    private Aggregation convertAggregation(ViewDefinitionSettings settings) {
        AggregationType type = settings.getAggregation();
        switch (type) {
            case SUM:
                return Aggregation.sum();
            case LAST_VALUE:
                return Aggregation.lastValue();
            case HISTOGRAM:
                return Aggregation.explicitBucketHistogram(settings.getBucketBoundaries());
            case EXPONENTIAL_HISTOGRAM:
                return Aggregation.base2ExponentialBucketHistogram(settings.getMaxBuckets(), settings.getMaxScale());
            default:
                throw new IllegalArgumentException("Unexpected OpenTelemetry aggregation:" + type);
        }
    }

    /**
     * @return the unit for the provided metric
     */
    private String getUnit(String metricName) {
        return env.getCurrentConfig().getMetrics().getDefinitions().get(metricName).getUnit();
    }
}

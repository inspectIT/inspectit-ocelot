package rocks.inspectit.ocelot.core.metrics.timewindow;

import io.opentelemetry.api.baggage.Baggage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.config.model.metrics.definition.views.ViewDefinitionSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.QuantilesView;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.SmoothedAverageView;
import rocks.inspectit.ocelot.core.metrics.timewindow.views.TimeWindowView;
import rocks.inspectit.ocelot.core.metrics.timewindow.worker.TimeWindowRecorder;
import rocks.inspectit.ocelot.core.attributes.CommonAttributesManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Allows the handling of time-window views on metrics.
 * Note that these views DO NOT coexist with OpenTelemetry {@link io.opentelemetry.sdk.metrics.View}s.
 * For this reason observations must be reported via {@link TimeWindowRecorder#recordMetric(String, double, Baggage)}
 * instead of using OpenTelemetry instruments.<br>
 */
@Slf4j
@Component
public class TimeWindowViewManager {

    @Autowired
    private InspectitEnvironment env;

    @Autowired
    private CommonAttributesManager commonAttributes;

    /**
     * Maps the name of measures to registered time-window views
     */
    private final Map<String, CopyOnWriteArrayList<TimeWindowView>> measuresToViewsMap = new ConcurrentHashMap<>();

    /**
     * @return the collection of registered time-window views for all metrics
     */
    public Collection<TimeWindowView> getAllViews() {
        return measuresToViewsMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * @param metricName the metric name
     *
     * @return the collection of registered time-window vies for the provided metric
     */
    public Collection<TimeWindowView> getViews(String metricName) {
        return measuresToViewsMap.get(metricName);
    }

    /**
     * @param metricName the name of the metric
     *
     * @return true, if any time-window view exists for the metric
     */
    public boolean areAnyViewsRegistered(String metricName) {
        return measuresToViewsMap.containsKey(metricName);
    }

    /**
     * Updates the custom time-window view in {@link #measuresToViewsMap} for the provided metric.
     *
     * @param metricName the metric name
     * @param viewName the view name
     * @param unit the metric unit
     * @param settings the (already validated) view settings
     */
    public synchronized void createOrUpdateView(String metricName, String viewName, String unit, ViewDefinitionSettings settings) {
        List<TimeWindowView> views = measuresToViewsMap.computeIfAbsent(metricName, (name) -> new CopyOnWriteArrayList<>());
        Optional<TimeWindowView> existingView = views.stream()
                .filter(view -> view.getViewName().equals(viewName))
                .findFirst();

        Optional<TimeWindowView> updatedView;
        if (existingView.isPresent()) {
            TimeWindowView currentView = existingView.get();
            updatedView = updateView(viewName, unit, settings, currentView);
        } else {
            updatedView = createView(viewName, unit, settings);
        }

        if (updatedView.isPresent()) {
            existingView.ifPresent(views::remove);
            views.add(updatedView.get());
        }
    }

    /**
     * Removes all views for the provided metric
     */
    public void removeViews(String metricName) {
        measuresToViewsMap.remove(metricName);
    }

    /**
     * Removes the provided view for the provided metric.
     * If there are no more views left for the metric, the metric will be removed entirely from {@link #measuresToViewsMap}.
     */
    public void removeView(String metricName, String viewName) {
        List<TimeWindowView> views = measuresToViewsMap.get(metricName);
        if (views != null) {
            Optional<TimeWindowView> existingView = views.stream()
                    .filter(view -> view.getViewName().equals(viewName))
                    .findFirst();

            if (existingView.isPresent()) {
                views.remove(existingView.get());
                if (views.isEmpty()) {
                    measuresToViewsMap.remove(metricName);
                }
            }
        }
    }

    /**
     * Updates the view, if there are any changes in the provided new settings
     *
     * @return the created view, if the existing view was updated, otherwise empty
     */
    private Optional<TimeWindowView> updateView(String viewName, String unit, ViewDefinitionSettings newSettings, TimeWindowView currentView) {
        switch (newSettings.getAggregation()) {
            case SMOOTHED_AVERAGE:
                return updateSmoothedAverageView(viewName, unit, newSettings, currentView);
            case QUANTILES:
                return updateQuantilesView(viewName, unit, newSettings, currentView);
            default:
                log.error("Could not update view '{}' with unknown time-window aggregation {}", viewName, newSettings.getAggregation());
                return Optional.empty();
        }
    }

    private Optional<TimeWindowView> updateSmoothedAverageView(String viewName, String unit, ViewDefinitionSettings newSettings, TimeWindowView currentView) {
        if (currentView instanceof QuantilesView) {
            return Optional.of(createQuantilesView(viewName, unit, newSettings));
        }

        SmoothedAverageView currentSmoothedAverageView = (SmoothedAverageView) currentView;
        String description = newSettings.getDescription();
        Set<String> attributeKeys = getAttributeKeysForView(newSettings);
        Duration timeWindow = newSettings.getTimeWindow();
        int bufferLimit = newSettings.getMaxBufferedPoints();
        double dropUpper = newSettings.getDropUpper();
        double dropLower = newSettings.getDropLower();

        if (!currentSmoothedAverageView.isSameConfiguration(description, unit, attributeKeys, timeWindow,
                bufferLimit, dropUpper, dropLower)) {
            return Optional.of(createSmoothedAverageView(viewName, unit, newSettings));
        }

        return Optional.empty();
    }

    private Optional<TimeWindowView> updateQuantilesView(String viewName, String unit, ViewDefinitionSettings newSettings, TimeWindowView currentView) {
        if (currentView instanceof SmoothedAverageView) {
            return Optional.of(createSmoothedAverageView(viewName, unit, newSettings));
        }

        QuantilesView currentQuantilesView = (QuantilesView) currentView;
        String description = newSettings.getDescription();
        Set<String> attributeKeys = getAttributeKeysForView(newSettings);
        Duration timeWindow = newSettings.getTimeWindow();
        int bufferLimit = newSettings.getMaxBufferedPoints();
        List<Double> quantiles = newSettings.getQuantiles();

        if (!currentQuantilesView.isSameConfiguration(description, unit, attributeKeys, timeWindow,
                bufferLimit, quantiles)) {
            return Optional.of(createQuantilesView(viewName, unit, newSettings));
        }

        return Optional.empty();
    }

    /**
     * @return the created view or empty, if the settings do not use a time-window aggregation
     */
    private Optional<TimeWindowView> createView(String viewName, String unit, ViewDefinitionSettings settings) {
        switch (settings.getAggregation()) {
            case SMOOTHED_AVERAGE:
                return Optional.of(createSmoothedAverageView(viewName, unit, settings));
            case QUANTILES:
                return Optional.of(createQuantilesView(viewName, unit, settings));
            default:
                log.error("Could not create view '{}' with unknown time-window aggregation {}", viewName, settings.getAggregation());
                return Optional.empty();
        }
    }

    private TimeWindowView createSmoothedAverageView(String viewName, String unit, ViewDefinitionSettings settings) {
        String description = settings.getDescription();
        Set<String> attributes = getAttributeKeysForView(settings);
        Duration timeWindow = settings.getTimeWindow();
        int bufferLimit = settings.getMaxBufferedPoints();
        double dropUpper = settings.getDropUpper();
        double dropLower = settings.getDropLower();

        return new SmoothedAverageView(viewName, description, unit, attributes, timeWindow, bufferLimit, dropUpper, dropLower);
    }

    private TimeWindowView createQuantilesView(String viewName, String unit, ViewDefinitionSettings settings) {
        String description = settings.getDescription();
        Set<String> attributes = getAttributeKeysForView(settings);
        Duration timeWindow = settings.getTimeWindow();
        int bufferLimit = settings.getMaxBufferedPoints();
        List<Double> quantiles = settings.getQuantiles();
        boolean includeMin = quantiles.contains(0.0);
        boolean includeMax = quantiles.contains(1.0);
        Set<Double> quantilesFiltered = quantiles.stream()
                .filter(p -> p > 0 && p < 1)
                .collect(Collectors.toSet());

        return new QuantilesView(viewName, description, unit, attributes, timeWindow, bufferLimit, quantilesFiltered, includeMin, includeMax);
    }

    // TODO We should move this method into another class
    /**
     * @return the attributes which are exposed for the given view
     */
    private Set<String> getAttributeKeysForView(ViewDefinitionSettings settings) {
        Set<String> viewTags = new HashSet<>();
        if (settings.isWithCommonAttributes()) {
            commonAttributes.getCommonAttributeKeys().stream()
                    .filter(attr -> settings.getAttributes().get(attr) != Boolean.FALSE)
                    .forEach(viewTags::add);
        }

        settings.getAttributes().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .forEach(viewTags::add);

        return viewTags;
    }
}

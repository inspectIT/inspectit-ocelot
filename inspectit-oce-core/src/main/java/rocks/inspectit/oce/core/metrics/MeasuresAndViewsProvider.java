package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagKey;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This class is responsible for creating and caching OpenCensus views and metrics.
 */
@Component
@Slf4j
public class MeasuresAndViewsProvider {

    @Autowired
    ViewManager viewManager;

    @Autowired
    CommonTagsManager commonTags;

    /**
     * Caches all creates long measures.
     */
    private ConcurrentHashMap<String, Measure.MeasureLong> measureLongs = new ConcurrentHashMap<>();

    /**
     * Caches all creates double measures.
     */
    private ConcurrentHashMap<String, Measure.MeasureDouble> measureDoubles = new ConcurrentHashMap<>();

    /**
     * Creates a new double measure and a new view of it.
     * This view only has the specified tags in it.
     * If you want to include common tags, use
     * {@link #getOrCreateMeasureDoubleWithViewAndCommonTags(String, String, String, Supplier, TagKey...)}
     *
     * @param name        the name of the measure and the view
     * @param description the description of the measure and the view
     * @param unit        the unit of the measure
     * @param aggregation the aggregation to use for the view
     * @param viewTags    the tags to use for the view
     * @return the newly created or already existing measure.
     */
    public Measure.MeasureDouble getOrCreateMeasureDoubleWithView(String name, String description, String unit, Supplier<Aggregation> aggregation, TagKey... viewTags) {
        return measureDoubles.computeIfAbsent(name, (n) -> {
            val measure = Measure.MeasureDouble.create(name, description, unit);
            List<TagKey> allTags = Arrays.asList(viewTags);
            View.Name viewName = View.Name.create(measure.getName());
            if (viewManager.getView(viewName) == null) {
                val view = View.create(viewName, measure.getDescription() + " [" + measure.getUnit() + "]",
                        measure, aggregation.get(), allTags);
                viewManager.registerView(view);
            } else {
                log.info("View with the name {} is already existent in OpenCensus, no new view is registered", name);
            }
            return measure;
        });
    }

    /**
     * Creates a new double measure and a new view of it.
     * This view includes the given tags AND the common tags provided by {@link CommonTagsManager}.
     * If you do not want to include common tags, use
     * {@link #getOrCreateMeasureDoubleWithView(String, String, String, Supplier, TagKey...)}
     *
     * @param name           the name of the measure and the view
     * @param description    the description of the measure and the view
     * @param unit           the unit of the measure
     * @param aggregation    the aggregation to use for the view
     * @param additionalTags the tags to use for the view in addition to the common tags
     * @return the newly created or already existing measure.
     */
    public Measure.MeasureDouble getOrCreateMeasureDoubleWithViewAndCommonTags(String name, String description, String unit, Supplier<Aggregation> aggregation, TagKey... additionalTags) {
        if (measureDoubles.containsKey(name)) {
            return measureDoubles.get(name);
        }
        List<TagKey> allTags = new ArrayList<>(commonTags.getCommonTagKeys());
        allTags.addAll(Arrays.asList(additionalTags));
        return getOrCreateMeasureDoubleWithView(name, description, unit, aggregation, allTags.toArray(new TagKey[]{}));
    }

    /**
     * Creates a new long measure and a new view of it.
     * This view only has the specified tags in it.
     * If you want to include common tags, use
     * {@link #getOrCreateMeasureDoubleWithViewAndCommonTags(String, String, String, Supplier, TagKey...)}
     *
     * @param name        the name of the measure and the view
     * @param description the description of the measure and the view
     * @param unit        the unit of the measure
     * @param aggregation the aggregation to use for the view
     * @param viewTags    the tags to use for the view
     * @return the newly created or already existing measure.
     */
    public Measure.MeasureLong getOrCreateMeasureLongWithView(String name, String description, String unit, Supplier<Aggregation> aggregation, TagKey... viewTags) {
        return measureLongs.computeIfAbsent(name, (n) -> {
            val measure = Measure.MeasureLong.create(name, description, unit);
            List<TagKey> allTags = Arrays.asList(viewTags);
            View.Name viewName = View.Name.create(measure.getName());
            if (viewManager.getView(viewName) == null) {
                val view = View.create(viewName, measure.getDescription() + " [" + measure.getUnit() + "]",
                        measure, aggregation.get(), allTags);
                viewManager.registerView(view);
            } else {
                log.info("View with the name {} is already existent in OpenCensus, no new view is registered", name);
            }
            return measure;
        });
    }

    /**
     * Creates a new long measure and a new view of it.
     * This view includes the given tags AND the common tags provided by {@link CommonTagsManager}.
     * If you do not want to include common tags, use
     * {@link #getOrCreateMeasureDoubleWithView(String, String, String, Supplier, TagKey...)}
     *
     * @param name           the name of the measure and the view
     * @param description    the description of the measure and the view
     * @param unit           the unit of the measure
     * @param aggregation    the aggregation to use for the view
     * @param additionalTags the tags to use for the view in addition to the common tags
     * @return the newly created or already existing measure.
     */
    public Measure.MeasureLong getOrCreateMeasureLongWithViewAndCommonTags(String name, String description, String unit, Supplier<Aggregation> aggregation, TagKey... additionalTags) {
        if (measureLongs.containsKey(name)) {
            return measureLongs.get(name);
        }
        List<TagKey> allTags = new ArrayList<>(commonTags.getCommonTagKeys());
        allTags.addAll(Arrays.asList(additionalTags));
        return getOrCreateMeasureLongWithView(name, description, unit, aggregation, allTags.toArray(new TagKey[]{}));
    }
}

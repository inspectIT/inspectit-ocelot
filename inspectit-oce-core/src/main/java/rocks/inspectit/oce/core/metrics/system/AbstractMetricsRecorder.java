package rocks.inspectit.oce.core.metrics.system;

import io.opencensus.stats.Measure;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;
import rocks.inspectit.oce.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.oce.core.service.DynamicallyActivatableService;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for dynamically enableable metrics recorders.
 */
@Slf4j
public abstract class AbstractMetricsRecorder extends DynamicallyActivatableService {

    @Autowired
    protected StatsRecorder recorder;

    @Autowired
    protected ViewManager viewManager;

    @Autowired
    protected MeasuresAndViewsManager measureManager;

    @Autowired
    protected CommonTagsManager commonTags;

    /**
     * Storage for lazily created long measures.
     */
    private Map<String, Measure.MeasureLong> createdMeasureLongs = new HashMap<>();

    /**
     * Storage for lazily created double measures.
     */
    private Map<String, Measure.MeasureDouble> createdMeasureDoubles = new HashMap<>();

    /**
     * Constructor.
     * This class already handles the dependency to the master switch.
     *
     * @param configDependency the configuration dependency of the recorder.
     */
    public AbstractMetricsRecorder(String configDependency) {
        super("metrics.enabled", configDependency);
    }

    @Override
    protected final boolean checkEnabledForConfig(InspectitConfig conf) {
        MetricsSettings ms = conf.getMetrics();
        return ms.isEnabled() && checkEnabledForConfig(ms);
    }

    /**
     * Checks if this recorder is active for the given configuration.
     * The master switch does not need to be checked, this is handled by {@link AbstractMetricsRecorder}.
     *
     * @param ms the metrics settings
     * @return true if the recorder should be enabled
     */
    protected abstract boolean checkEnabledForConfig(MetricsSettings ms);

}

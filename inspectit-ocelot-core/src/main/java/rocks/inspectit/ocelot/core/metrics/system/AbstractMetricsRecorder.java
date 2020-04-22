package rocks.inspectit.ocelot.core.metrics.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.core.metrics.MeasuresAndViewsManager;
import rocks.inspectit.ocelot.core.service.DynamicallyActivatableService;
import rocks.inspectit.ocelot.core.tags.CommonTagsManager;

/**
 * Base class for dynamically enableable metrics recorders.
 */
@Slf4j
public abstract class AbstractMetricsRecorder extends DynamicallyActivatableService {

    @Autowired
    protected MeasuresAndViewsManager measureManager;

    @Autowired
    protected CommonTagsManager commonTags;

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
     *
     * @return true if the recorder should be enabled
     */
    protected abstract boolean checkEnabledForConfig(MetricsSettings ms);

}

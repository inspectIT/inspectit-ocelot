package rocks.inspectit.ocelot.core.metrics.system;

import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;

@Service
public class ClassLoaderMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "jvm/classes/";

    private static final String LOADED_METRIC_NAME = "loaded";

    private static final String UNLOADED_METRIC_NAME = "unloaded";

    private ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();

    public ClassLoaderMetricsRecorder() {
        super("metrics.classloader");
    }

    @Override
    protected void takeMeasurement(MetricsSettings config) {
        val mm = recorder.newMeasureMap();
        val cl = config.getClassloader();
        if (cl.getEnabled().getOrDefault(LOADED_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + LOADED_METRIC_NAME, mm,
                    classLoadingBean.getLoadedClassCount());
        }
        if (cl.getEnabled().getOrDefault(UNLOADED_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + UNLOADED_METRIC_NAME, mm,
                    classLoadingBean.getUnloadedClassCount());
        }
        mm.record();
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getClassloader().getFrequency();
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        return ms.getClassloader().getEnabled().containsValue(true);
    }
}

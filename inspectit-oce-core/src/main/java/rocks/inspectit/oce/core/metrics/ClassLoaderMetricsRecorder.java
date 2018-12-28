package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.MeasureMap;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;

@Service
public class ClassLoaderMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "jvm/classes/";

    private static final String LOADED_METRIC_NAME = "loaded";
    private static final String LOADED_METRIC_DESCRIPTION = "Total number of classes currently loaded in the JVM";

    private static final String UNLOADED_METRIC_NAME = "unloaded";
    private static final String UNLOADED_METRIC_DESCRIPTION = "Total number of classes which have been unloaded since the start of the JVM";

    private static final String METRIC_UNIT = "1";

    private ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();

    public ClassLoaderMetricsRecorder() {
        super("metrics.classloader");
    }

    @Override
    protected void takeMeasurement(MetricsSettings config, MeasureMap mm) {
        val cl = config.getClassloader();
        if (cl.getEnabled().getOrDefault(LOADED_METRIC_NAME, false)) {
            val measure = getOrCreateMeasureLongWithView(METRIC_NAME_PREFIX + LOADED_METRIC_NAME,
                    LOADED_METRIC_DESCRIPTION, METRIC_UNIT, Aggregation.LastValue::create);
            mm.put(measure, classLoadingBean.getLoadedClassCount());
        }
        if (cl.getEnabled().getOrDefault(UNLOADED_METRIC_NAME, false)) {
            val measure = getOrCreateMeasureLongWithView(METRIC_NAME_PREFIX + UNLOADED_METRIC_NAME,
                    UNLOADED_METRIC_DESCRIPTION, METRIC_UNIT, Aggregation.LastValue::create);
            mm.put(measure, classLoadingBean.getUnloadedClassCount());
        }
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

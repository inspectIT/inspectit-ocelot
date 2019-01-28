package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

import java.io.File;
import java.time.Duration;

@Service
@Slf4j
public class DiskMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "disk/";

    private static final String FREE_METRIC_NAME = "free";
    private static final String FREE_METRIC_DESCRIPTION = "Free disk space";

    private static final String TOTAL_METRIC_NAME = "total";
    private static final String TOTAL_METRIC_DESCRIPTION = "Total disk space";

    private static final String METRIC_UNIT = "bytes";

    public DiskMetricsRecorder() {
        super("metrics.disk");
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings conf) {
        return conf.getDisk().getEnabled().containsValue(true);
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getDisk().getFrequency();
    }

    @Override
    protected void takeMeasurement(MetricsSettings config) {
        val mm = recorder.newMeasureMap();
        val disk = config.getDisk();
        if (disk.getEnabled().getOrDefault(FREE_METRIC_NAME, false)) {
            Measure.MeasureLong free = measureProvider.getOrCreateMeasureLongWithViewAndCommonTags(METRIC_NAME_PREFIX + FREE_METRIC_NAME,
                    FREE_METRIC_DESCRIPTION, METRIC_UNIT, Aggregation.LastValue::create);
            mm.put(free, new File("/").getFreeSpace());
        }
        if (disk.getEnabled().getOrDefault(TOTAL_METRIC_NAME, false)) {
            Measure.MeasureLong free = measureProvider.getOrCreateMeasureLongWithViewAndCommonTags(METRIC_NAME_PREFIX + TOTAL_METRIC_NAME,
                    TOTAL_METRIC_DESCRIPTION, METRIC_UNIT, Aggregation.LastValue::create);
            mm.put(free, new File("/").getTotalSpace());
        }
        mm.record();
    }
}

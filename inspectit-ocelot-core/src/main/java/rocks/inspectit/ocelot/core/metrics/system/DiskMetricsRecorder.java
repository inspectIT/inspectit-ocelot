package rocks.inspectit.ocelot.core.metrics.system;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

import java.io.File;
import java.time.Duration;

@Service
@Slf4j
public class DiskMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "disk/";

    private static final String FREE_METRIC_NAME = "free";

    private static final String TOTAL_METRIC_NAME = "total";

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
        val disk = config.getDisk();
        if (disk.getEnabled().getOrDefault(FREE_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + FREE_METRIC_NAME, new File("/").getFreeSpace());
        }
        if (disk.getEnabled().getOrDefault(TOTAL_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + TOTAL_METRIC_NAME, new File("/").getTotalSpace());
        }
    }
}

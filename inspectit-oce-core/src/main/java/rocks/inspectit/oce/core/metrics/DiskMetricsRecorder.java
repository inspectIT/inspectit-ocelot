package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.*;
import io.opencensus.tags.TagContext;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.InspectitConfig;
import rocks.inspectit.oce.core.service.DynamicallyActivatableService;
import rocks.inspectit.oce.core.tags.CommonTagsManager;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DiskMetricsRecorder extends DynamicallyActivatableService {


    private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();

    @Autowired
    protected ScheduledExecutorService executor;

    @Autowired
    protected CommonTagsManager commonTagsManager;

    private Measure.MeasureLong freeDiskSpaceMeasure;
    private Measure.MeasureLong totalDiskSpaceMeasure;
    private ScheduledFuture<?> pollingTask;

    public DiskMetricsRecorder() {
        super("metrics.enabled", "metrics.disk");
    }

    /**
     * Lazily initalizes the free disk space measure and its view on first call.
     *
     * @return the measure for the free disk space
     */
    private Measure.MeasureLong getFreeDiskSpaceMeasure() {
        if (freeDiskSpaceMeasure == null) {
            freeDiskSpaceMeasure = Measure.MeasureLong.create("disk/free", "Free disk space", "bytes");
            Stats.getViewManager().registerView(
                    View.create(View.Name.create(freeDiskSpaceMeasure.getName()),
                            freeDiskSpaceMeasure.getDescription(),
                            freeDiskSpaceMeasure, Aggregation.LastValue.create(), commonTagsManager.getCommonTagKeys())
            );
        }
        return freeDiskSpaceMeasure;
    }

    /**
     * Lazily initalizes the total disk space measure and its view on first call.
     *
     * @return the measure for the total disk space
     */
    private Measure.MeasureLong getTotalDiskSpaceMeasure() {
        if (totalDiskSpaceMeasure == null) {
            totalDiskSpaceMeasure = Measure.MeasureLong.create("disk/total", "Total disk space", "bytes");
            Stats.getViewManager().registerView(
                    View.create(View.Name.create(totalDiskSpaceMeasure.getName()),
                            totalDiskSpaceMeasure.getDescription(),
                            totalDiskSpaceMeasure, Aggregation.LastValue.create(), commonTagsManager.getCommonTagKeys())
            );
        }
        return totalDiskSpaceMeasure;
    }

    @Override

    protected boolean checkEnabledForConfig(InspectitConfig conf) {
        val metrics = conf.getMetrics();
        return metrics.isEnabled()
                && (metrics.getDisk().isFree() || metrics.getDisk().isTotal());
    }

    @Override
    protected boolean doEnable(InspectitConfig configuration) {
        log.info("Enabling disk space recorder.");
        val conf = configuration.getMetrics().getDisk();
        pollingTask = executor.scheduleWithFixedDelay(() -> {
            val mm = STATS_RECORDER.newMeasureMap();
            if (conf.isFree()) {
                mm.put(getFreeDiskSpaceMeasure(), new File("/").getFreeSpace());
            }
            if (conf.isTotal()) {
                mm.put(getTotalDiskSpaceMeasure(), new File("/").getTotalSpace());
            }
            TagContext commonTagContext = commonTagsManager.getCommonTagContext();
            mm.record(commonTagContext);
        }, conf.getFrequencyMs(), conf.getFrequencyMs(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    protected boolean doDisable() {
        log.info("Disabling disk space recorder.");
        pollingTask.cancel(true);
        return true;
    }
}

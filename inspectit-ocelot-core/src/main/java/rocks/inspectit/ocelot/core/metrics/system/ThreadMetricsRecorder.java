package rocks.inspectit.ocelot.core.metrics.system;

import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

@Service
public class ThreadMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "jvm/threads/";

    private static final String PEAK_METRIC_NAME = "peak";

    private static final String DAEMON_METRIC_NAME = "daemon";

    private static final String LIVE_METRIC_NAME = "live";

    private static final String STATE_METRIC_NAME = "states";

    private static final String STATE_TAG_NAME = "state";

    private TagKey stateTag;

    private ThreadMXBean threadBean;

    @Autowired
    private Tagger tagger;

    public ThreadMetricsRecorder() {
        super("metrics.threads");
        stateTag = TagKey.create(STATE_TAG_NAME);
        threadBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    protected void takeMeasurement(MetricsSettings config) {
        val measurement = recorder.newMeasureMap();
        val enabled = config.getThreads().getEnabled();
        if (enabled.getOrDefault(PEAK_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + PEAK_METRIC_NAME, measurement,
                    threadBean.getPeakThreadCount());
        }
        if (enabled.getOrDefault(DAEMON_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + DAEMON_METRIC_NAME, measurement,
                    threadBean.getDaemonThreadCount());
        }
        if (enabled.getOrDefault(LIVE_METRIC_NAME, false)) {
            measureManager.tryRecordingMeasurement(METRIC_NAME_PREFIX + LIVE_METRIC_NAME, measurement,
                    threadBean.getThreadCount());
        }
        if (enabled.getOrDefault(STATE_METRIC_NAME, false)) {
            recordStateMetric();
        }
        measurement.record();
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getThreads().getFrequency();
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        return ms.getThreads().getEnabled().containsValue(true);
    }

    private void recordStateMetric() {
        measureManager.getMeasureLong(METRIC_NAME_PREFIX + STATE_METRIC_NAME)
                .ifPresent(measure -> {
                    for (val state : Thread.State.values()) {
                        TagContextBuilder contextBuilder = tagger.currentBuilder().putLocal(stateTag, TagValue.create(state.name()));
                        try (val scope = contextBuilder.buildScoped()) {
                            val mm = recorder.newMeasureMap();
                            val count = Arrays.stream(threadBean.getThreadInfo(threadBean.getAllThreadIds()))
                                    .filter(Objects::nonNull)
                                    .map(ThreadInfo::getThreadState)
                                    .filter(s -> s == state)
                                    .count();
                            mm.put(measure, count);
                            mm.record();
                        }
                    }
                });
    }
}

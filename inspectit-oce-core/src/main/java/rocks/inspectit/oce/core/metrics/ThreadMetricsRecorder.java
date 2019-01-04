package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.MeasureMap;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;

@Service
public class ThreadMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String METRIC_NAME_PREFIX = "jvm/threads/";

    private static final String PEAK_METRIC_NAME = "peak";
    private static final String PEAK_METRIC_DESCRIPTION = "Peak number of live threads since the JVM start";

    private static final String DAEMON_METRIC_NAME = "daemon";
    private static final String DAEMON_METRIC_DESCRIPTION = "Current number of live daemon threads";

    private static final String LIVE_METRIC_NAME = "live";
    private static final String LIVE_METRIC_DESCRIPTION = "Current number of total live threads";

    private static final String STATE_METRIC_NAME = "states";
    private static final String STATE_METRIC_DESCRIPTION = "Number of live threads for each state";

    private static final String STATE_TAG_NAME = "state";
    private static final String METRIC_THREAD_UNIT = "threads";

    private TagKey stateTag;

    private ThreadMXBean threadBean;

    @Autowired
    Tagger tagger;

    public ThreadMetricsRecorder() {
        super("metrics.threads");
        stateTag = TagKey.create(STATE_TAG_NAME);
        threadBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    protected void takeMeasurement(MetricsSettings config, MeasureMap measurement) {
        val enabled = config.getThreads().getEnabled();
        if (enabled.getOrDefault(PEAK_METRIC_NAME, false)) {
            val measure = getOrCreateMeasureLongWithView(METRIC_NAME_PREFIX + PEAK_METRIC_NAME,
                    PEAK_METRIC_DESCRIPTION, METRIC_THREAD_UNIT, Aggregation.LastValue::create);
            measurement.put(measure, threadBean.getPeakThreadCount());
        }
        if (enabled.getOrDefault(DAEMON_METRIC_NAME, false)) {
            val measure = getOrCreateMeasureLongWithView(METRIC_NAME_PREFIX + DAEMON_METRIC_NAME,
                    DAEMON_METRIC_DESCRIPTION, METRIC_THREAD_UNIT, Aggregation.LastValue::create);
            measurement.put(measure, threadBean.getDaemonThreadCount());
        }
        if (enabled.getOrDefault(LIVE_METRIC_NAME, false)) {
            val measure = getOrCreateMeasureLongWithView(METRIC_NAME_PREFIX + LIVE_METRIC_NAME,
                    LIVE_METRIC_DESCRIPTION, METRIC_THREAD_UNIT, Aggregation.LastValue::create);
            measurement.put(measure, threadBean.getThreadCount());
        }
        if (enabled.getOrDefault(STATE_METRIC_NAME, false)) {
            recordStateMetric();
        }
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
        val measure = getOrCreateMeasureLongWithView(METRIC_NAME_PREFIX + STATE_METRIC_NAME,
                STATE_METRIC_DESCRIPTION, METRIC_THREAD_UNIT, Aggregation.LastValue::create, stateTag);
        for (val state : Thread.State.values()) {
            TagContextBuilder contextBuilder = tagger.currentBuilder().put(stateTag, TagValue.create(state.name()));
            try (val scope = contextBuilder.buildScoped()) {
                val mm = recorder.newMeasureMap();
                val count = Arrays.stream(threadBean.getThreadInfo(threadBean.getAllThreadIds()))
                        .filter(info -> info != null && info.getThreadState() == state)
                        .count();
                mm.put(measure, count);
                mm.record();
            }
        }
    }
}

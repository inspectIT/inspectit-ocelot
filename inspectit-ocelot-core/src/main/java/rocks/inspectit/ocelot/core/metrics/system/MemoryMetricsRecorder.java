package rocks.inspectit.ocelot.core.metrics.system;

import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.time.Duration;
import java.util.Map;

@Service
public class MemoryMetricsRecorder extends AbstractPollingMetricsRecorder {


    private static final String USED_METRIC_NAME = "used";
    private static final String USED_METRIC_FULL_NAME = "jvm/memory/used";

    private static final String COMMITTED_METRIC_NAME = "committed";
    private static final String COMMITTED_METRIC_FULL_NAME = "jvm/memory/committed";

    private static final String MAX_METRIC_NAME = "max";
    private static final String MAX_METRIC_FULL_NAME = "jvm/memory/max";

    private static final String BUFFER_COUNT_METRIC_NAME = "buffer.count";
    private static final String BUFFER_COUNT_METRIC_FULL_NAME = "jvm/buffer/count";

    private static final String BUFFER_USED_METRIC_NAME = "buffer.used";
    private static final String BUFFER_USED_METRIC_FULL_NAME = "jvm/buffer/memory/used";

    private static final String BUFFER_CAPACITY_METRIC_NAME = "buffer.capacity";
    private static final String BUFFER_CAPACITY_METRIC_FULL_NAME = "jvm/buffer/total/capacity";

    private TagKey idTagKey = TagKey.create("id");
    private TagKey areaTagKey = TagKey.create("area");

    @Autowired
    private Tagger tagger;

    public MemoryMetricsRecorder() {
        super("metrics.memory");
    }

    @Override
    protected void takeMeasurement(MetricsSettings config) {
        val enabled = config.getMemory().getEnabled();
        recordMemoryMetrics(enabled);
        recordBufferMetrics(enabled);
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getMemory().getFrequency();
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        return ms.getMemory().getEnabled().containsValue(true);
    }

    private void recordMemoryMetrics(Map<String, Boolean> enabledMetrics) {
        boolean usedEnabled = enabledMetrics.getOrDefault(USED_METRIC_NAME, false);
        boolean committedEnabled = enabledMetrics.getOrDefault(COMMITTED_METRIC_NAME, false);
        boolean maxEnabled = enabledMetrics.getOrDefault(MAX_METRIC_NAME, false);
        if (usedEnabled || committedEnabled || maxEnabled) {
            for (MemoryPoolMXBean memoryPoolBean : ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class)) {
                String area = MemoryType.HEAP.equals(memoryPoolBean.getType()) ? "heap" : "nonheap";
                TagContext tags = tagger.currentBuilder()
                        .putLocal(idTagKey, TagValue.create(memoryPoolBean.getName()))
                        .putLocal(areaTagKey, TagValue.create(area))
                        .build();

                val mm = recorder.newMeasureMap();
                if (usedEnabled) {
                    measureManager.tryRecordingMeasurement(USED_METRIC_FULL_NAME, mm, memoryPoolBean.getUsage().getUsed());
                }
                if (committedEnabled) {
                    measureManager.tryRecordingMeasurement(COMMITTED_METRIC_FULL_NAME, mm, memoryPoolBean.getUsage().getCommitted());
                }
                if (maxEnabled) {
                    long max = memoryPoolBean.getUsage().getMax();
                    if (max == -1) { //max memory not set
                        max = 0L; //negative values are not supported by OpenCensus
                    }
                    measureManager.tryRecordingMeasurement(MAX_METRIC_FULL_NAME, mm, max);

                }
                mm.record(tags);
            }
        }
    }

    private void recordBufferMetrics(Map<String, Boolean> enabledMetrics) {
        boolean bufferCountEnabled = enabledMetrics.getOrDefault(BUFFER_COUNT_METRIC_NAME, false);
        boolean bufferUsedEnabled = enabledMetrics.getOrDefault(BUFFER_USED_METRIC_NAME, false);
        boolean bufferCapacityEnabled = enabledMetrics.getOrDefault(BUFFER_CAPACITY_METRIC_NAME, false);
        if (bufferCountEnabled || bufferUsedEnabled || bufferCapacityEnabled) {
            for (BufferPoolMXBean bufferPoolBean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
                TagContext tags = tagger.currentBuilder()
                        .putLocal(idTagKey, TagValue.create(bufferPoolBean.getName()))
                        .build();

                val mm = recorder.newMeasureMap();
                if (bufferCountEnabled) {
                    measureManager.tryRecordingMeasurement(BUFFER_COUNT_METRIC_FULL_NAME, mm, bufferPoolBean.getCount());
                }
                if (bufferUsedEnabled) {
                    measureManager.tryRecordingMeasurement(BUFFER_USED_METRIC_FULL_NAME, mm, bufferPoolBean.getMemoryUsed());
                }
                if (bufferCapacityEnabled) {
                    measureManager.tryRecordingMeasurement(BUFFER_CAPACITY_METRIC_FULL_NAME, mm, bufferPoolBean.getTotalCapacity());
                }
                mm.record(tags);
            }
        }
    }
}

package rocks.inspectit.oce.core.metrics;

import io.opencensus.stats.Aggregation;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.oce.core.config.model.metrics.MetricsSettings;

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
    private static final String USED_METRIC_DESCRIPTION = "The amount of used memory";
    private static final String USED_UNIT = "bytes";

    private static final String COMMITTED_METRIC_NAME = "committed";
    private static final String COMMITTED_METRIC_FULL_NAME = "jvm/memory/committed";
    private static final String COMMITTED_METRIC_DESCRIPTION = "The amount of memory in bytes that is committed for the Java virtual machine to use";
    private static final String COMMITTED_UNIT = "bytes";

    private static final String MAX_METRIC_NAME = "max";
    private static final String MAX_METRIC_FULL_NAME = "jvm/memory/max";
    private static final String MAX_METRIC_DESCRIPTION = "The maximum amount of memory in bytes that can be used for memory management";
    private static final String MAX_UNIT = "bytes";

    private static final String BUFFER_COUNT_METRIC_NAME = "buffer.count";
    private static final String BUFFER_COUNT_METRIC_FULL_NAME = "jvm/buffer/count";
    private static final String BUFFER_COUNT_METRIC_DESCRIPTION = "An estimate of the number of buffers in the pool";
    private static final String BUFFER_COUNT_UNIT = "buffers";

    private static final String BUFFER_USED_METRIC_NAME = "buffer.used";
    private static final String BUFFER_USED_METRIC_FULL_NAME = "jvm/buffer/memory/used";
    private static final String BUFFER_USED_METRIC_DESCRIPTION = "An estimate of the memory that the Java virtual machine is using for this buffer pool";
    private static final String BUFFER_USED_UNIT = "bytes";

    private static final String BUFFER_CAPACITY_METRIC_NAME = "buffer.capacity";
    private static final String BUFFER_CAPACITY_METRIC_FULL_NAME = "jvm/buffer/total/capacity";
    private static final String BUFFER_CAPACITY_METRIC_DESCRIPTION = "An estimate of the total capacity of the buffers in this pool";
    private static final String BUFFER_CAPACITY_UNIT = "bytes";

    private TagKey idTagKey = TagKey.create("id");
    private TagKey areaTagKey = TagKey.create("area");

    @Autowired
    Tagger tagger;

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
                        .put(idTagKey, TagValue.create(memoryPoolBean.getName()))
                        .put(areaTagKey, TagValue.create(area))
                        .build();

                val mm = recorder.newMeasureMap();
                if (usedEnabled) {
                    val usedM = getOrCreateMeasureLongWithView(USED_METRIC_FULL_NAME,
                            USED_METRIC_DESCRIPTION, USED_UNIT, Aggregation.LastValue::create,
                            idTagKey, areaTagKey);
                    mm.put(usedM, memoryPoolBean.getUsage().getUsed());
                }
                if (committedEnabled) {
                    val committedM = getOrCreateMeasureLongWithView(COMMITTED_METRIC_FULL_NAME,
                            COMMITTED_METRIC_DESCRIPTION, COMMITTED_UNIT, Aggregation.LastValue::create,
                            idTagKey, areaTagKey);
                    mm.put(committedM, memoryPoolBean.getUsage().getCommitted());
                }
                if (maxEnabled) {
                    val maxM = getOrCreateMeasureLongWithView(MAX_METRIC_FULL_NAME,
                            MAX_METRIC_DESCRIPTION, MAX_UNIT, Aggregation.LastValue::create,
                            idTagKey, areaTagKey);
                    long max = memoryPoolBean.getUsage().getMax();
                    if (max == -1) { //max memory not set
                        mm.put(maxM, 0L); //negative values are not supported by OpenCensus
                    } else {
                        mm.put(maxM, max);
                    }
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
                        .put(idTagKey, TagValue.create(bufferPoolBean.getName()))
                        .build();

                val mm = recorder.newMeasureMap();
                if (bufferCountEnabled) {
                    val countM = getOrCreateMeasureLongWithView(BUFFER_COUNT_METRIC_FULL_NAME,
                            BUFFER_COUNT_METRIC_DESCRIPTION, BUFFER_COUNT_UNIT, Aggregation.LastValue::create,
                            idTagKey);
                    mm.put(countM, bufferPoolBean.getCount());
                }
                if (bufferUsedEnabled) {
                    val usedM = getOrCreateMeasureLongWithView(BUFFER_USED_METRIC_FULL_NAME,
                            BUFFER_USED_METRIC_DESCRIPTION, BUFFER_USED_UNIT, Aggregation.LastValue::create,
                            idTagKey);
                    mm.put(usedM, bufferPoolBean.getMemoryUsed());
                }
                if (bufferCapacityEnabled) {
                    val capacityM = getOrCreateMeasureLongWithView(BUFFER_CAPACITY_METRIC_FULL_NAME,
                            BUFFER_CAPACITY_METRIC_DESCRIPTION, BUFFER_CAPACITY_UNIT, Aggregation.LastValue::create,
                            idTagKey);
                    mm.put(capacityM, bufferPoolBean.getTotalCapacity());
                }
                mm.record(tags);
            }
        }
    }
}

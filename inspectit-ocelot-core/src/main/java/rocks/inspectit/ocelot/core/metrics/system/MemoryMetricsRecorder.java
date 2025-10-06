package rocks.inspectit.ocelot.core.metrics.system;

import io.opentelemetry.api.baggage.Baggage;
import lombok.val;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.core.utils.AttributeUtils;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.time.Duration;
import java.util.Map;

@Service
public class MemoryMetricsRecorder extends AbstractPollingMetricsRecorder {

    private static final String USED_METRIC_NAME = "used";

    private static final String USED_METRIC_FULL_NAME = "jvm_memory_used";

    private static final String COMMITTED_METRIC_NAME = "committed";

    private static final String COMMITTED_METRIC_FULL_NAME = "jvm_memory_committed";

    private static final String MAX_METRIC_NAME = "max";

    private static final String MAX_METRIC_FULL_NAME = "jvm_memory_max";

    private static final String BUFFER_COUNT_METRIC_NAME = "buffer.count";

    private static final String BUFFER_COUNT_METRIC_FULL_NAME = "jvm_buffer_count";

    private static final String BUFFER_USED_METRIC_NAME = "buffer.used";

    private static final String BUFFER_USED_METRIC_FULL_NAME = "jvm_buffer_memory_used";

    private static final String BUFFER_CAPACITY_METRIC_NAME = "buffer.capacity";

    private static final String BUFFER_CAPACITY_METRIC_FULL_NAME = "jvm_buffer_total_capacity";

    private final String idKey = "id";

    private final String areaKey = "area";

    public MemoryMetricsRecorder() {
        super("metrics.memory");
    }

    @Override
    protected void takeMetric(MetricsSettings config) {
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
                Baggage baggage = Baggage.current().toBuilder()
                        .put(idKey, AttributeUtils.resolveValue(idKey, memoryPoolBean.getName()))
                        .put(areaKey, AttributeUtils.resolveValue(areaKey, area))
                        .build();

                if (usedEnabled) {
                    instrumentManager.tryRecordingMetric(USED_METRIC_FULL_NAME, memoryPoolBean.getUsage()
                            .getUsed(), baggage);
                }
                if (committedEnabled) {
                    instrumentManager.tryRecordingMetric(COMMITTED_METRIC_FULL_NAME, memoryPoolBean.getUsage()
                            .getCommitted(), baggage);
                }
                if (maxEnabled) {
                    long max = memoryPoolBean.getUsage().getMax();
                    if (max == -1) { // max memory not set
                        max = 0L;    // negative values are not supported
                    }
                    instrumentManager.tryRecordingMetric(MAX_METRIC_FULL_NAME, max, baggage);

                }
            }
        }
    }

    private void recordBufferMetrics(Map<String, Boolean> enabledMetrics) {
        boolean bufferCountEnabled = enabledMetrics.getOrDefault(BUFFER_COUNT_METRIC_NAME, false);
        boolean bufferUsedEnabled = enabledMetrics.getOrDefault(BUFFER_USED_METRIC_NAME, false);
        boolean bufferCapacityEnabled = enabledMetrics.getOrDefault(BUFFER_CAPACITY_METRIC_NAME, false);
        if (bufferCountEnabled || bufferUsedEnabled || bufferCapacityEnabled) {
            for (BufferPoolMXBean bufferPoolBean : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
                Baggage baggage = Baggage.current().toBuilder()
                        .put(idKey, AttributeUtils.resolveValue(idKey, bufferPoolBean.getName()))
                        .build();

                if (bufferCountEnabled) {
                    instrumentManager.tryRecordingMetric(BUFFER_COUNT_METRIC_FULL_NAME, bufferPoolBean.getCount(), baggage);
                }
                if (bufferUsedEnabled) {
                    instrumentManager.tryRecordingMetric(BUFFER_USED_METRIC_FULL_NAME, bufferPoolBean.getMemoryUsed(), baggage);
                }
                if (bufferCapacityEnabled) {
                    instrumentManager.tryRecordingMetric(BUFFER_CAPACITY_METRIC_FULL_NAME, bufferPoolBean.getTotalCapacity(), baggage);
                }
            }
        }
    }
}

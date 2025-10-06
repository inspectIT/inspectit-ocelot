package rocks.inspectit.ocelot.core.metrics.concurrent;

import io.opentelemetry.api.baggage.Baggage;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rocks.inspectit.ocelot.config.model.metrics.MetricsSettings;
import rocks.inspectit.ocelot.core.metrics.system.AbstractPollingMetricsRecorder;
import rocks.inspectit.ocelot.core.utils.AttributeUtils;

import java.time.Duration;

/**
 * Records metrics regarding concurrency. At the moment we only record concurrent invocations of specific operations.
 * The invocations will be counted by enabling concurrent-invocations within rules.
 */
@Service
public class ConcurrentMetricsRecorder extends AbstractPollingMetricsRecorder {

    /**
     *  See {@code system-and-jvm-metrics.yml} in default configuration
     */
    private static final String METRIC_NAME_PREFIX = "inspectit_concurrent_";

    private static final String INVOCATIONS_METRIC_NAME = "invocations";

    private final String attributeKey = "operation";

    @Autowired
    private ConcurrentInvocationManager concurrentInvocations;

    public ConcurrentMetricsRecorder() {
        super("metrics.concurrent");
    }

    @Override
    protected void takeMetric(MetricsSettings config) {
        val enabled = config.getConcurrent().getEnabled();
        if (enabled.getOrDefault(INVOCATIONS_METRIC_NAME, false)) {
            String metricName = METRIC_NAME_PREFIX + INVOCATIONS_METRIC_NAME;

            concurrentInvocations.getActiveInvocations().forEach((operation, count) -> {
                Baggage baggage = Baggage.current().toBuilder()
                        .put(attributeKey, AttributeUtils.resolveValue(attributeKey, operation))
                        .build();

                instrumentManager.tryRecordingMetric(metricName, count, baggage);
            });
        }
    }

    @Override
    protected Duration getFrequency(MetricsSettings config) {
        return config.getConcurrent().getFrequency();
    }

    @Override
    protected boolean checkEnabledForConfig(MetricsSettings ms) {
        return ms.getConcurrent().getEnabled().containsValue(true);
    }
}

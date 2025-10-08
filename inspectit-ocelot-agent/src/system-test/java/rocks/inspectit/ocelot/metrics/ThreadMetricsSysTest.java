package rocks.inspectit.ocelot.metrics;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ThreadMetricsSysTest extends MetricsSysTestBase {

    @Test
    public void testThreadMetricsCapturing() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {

            LongPointData liveData = (LongPointData) MetricTestUtils.getDataForView("jvm_threads_live", emptyMap());
            LongPointData peakData = (LongPointData) MetricTestUtils.getDataForView("jvm_threads_peak", emptyMap());
            LongPointData daemonData = (LongPointData) MetricTestUtils.getDataForView("jvm_threads_daemon", emptyMap());
            Collection<PointData> stateData = MetricTestUtils.getAllDataForView("jvm_threads_states");

            assertThat(liveData).isNotNull();
            assertThat(peakData).isNotNull();
            assertThat(daemonData).isNotNull();
            assertThat(stateData).isNotEmpty();

            assertThat(liveData.getAttributes().asMap()).isNotEmpty();
            assertThat(peakData.getAttributes().asMap()).isNotEmpty();
            assertThat(daemonData.getAttributes().asMap()).isNotEmpty();
            assertThat(stateData.stream().findFirst().get().getAttributes().asMap()).isNotEmpty();

            long live = liveData.getValue();
            long peak = peakData.getValue();
            long daemon = daemonData.getValue();
            long statesCount = stateData.stream()
                    .map((pointData -> (LongPointData) pointData))
                            .mapToLong((LongPointData::getValue))
                            .sum();

            assertThat(live).isEqualTo(statesCount);
            assertThat(peak).isGreaterThanOrEqualTo(live);
            assertThat(daemon).isLessThan(live);
        });
    }
}

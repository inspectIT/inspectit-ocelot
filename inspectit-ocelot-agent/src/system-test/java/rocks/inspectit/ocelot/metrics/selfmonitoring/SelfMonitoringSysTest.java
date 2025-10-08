package rocks.inspectit.ocelot.metrics.selfmonitoring;

import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.metrics.MetricsSysTestBase;
import rocks.inspectit.ocelot.utils.MetricTestUtils;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SelfMonitoringSysTest extends MetricsSysTestBase {

    @Test
    void metricsRecorders() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            Collection<PointData> inspectItDuration = MetricTestUtils.getAllDataForView("inspectit_self_duration");

            assertThat(inspectItDuration).isNotEmpty();
            assertThat(inspectItDuration.stream().findFirst().get().getAttributes().asMap()).isNotEmpty();

            // check for components
            assertThat(inspectItDuration)
                    .anyMatch((pointData -> containsAttributeValue(pointData, "ProcessorMetricsRecorder")));
            assertThat(inspectItDuration)
                    .anyMatch((pointData -> containsAttributeValue(pointData, "ClassLoaderMetricsRecorder")));
            assertThat(inspectItDuration)
                    .anyMatch((pointData -> containsAttributeValue(pointData, "DiskMetricsRecorder")));

            double sum = inspectItDuration.stream()
                    .map((pointData -> (DoublePointData) pointData))
                    .mapToDouble((DoublePointData::getValue))
                    .sum();

            assertThat(sum).isNotNegative().isNotZero();
        });
    }

    /**
     * Checks, if the point data contains the provided attribute value
     */
    private static boolean containsAttributeValue(PointData pointData, String attrValue) {
        return pointData.getAttributes().asMap().containsValue(attrValue);
    }
}

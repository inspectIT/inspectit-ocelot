package rocks.inspectit.ocelot.metrics;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ClassLoaderMetricsSysTest extends MetricsSysTestBase {

    @Test
    public void testClassLoaderMetricCapturing() {
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            LongPointData loadedData = (LongPointData) MetricTestUtils.getDataForView("jvm_classes_loaded", emptyMap());
            LongPointData unloadedData = (LongPointData) MetricTestUtils.getDataForView("jvm_classes_unloaded", emptyMap());

            assertThat(loadedData).isNotNull();
            assertThat(unloadedData).isNotNull();

            // ensure that attributes are present
            assertThat(loadedData.getAttributes().asMap()).isNotEmpty();
            assertThat(unloadedData.getAttributes().asMap()).isNotEmpty();

            // ensure that the values are sane
            long loaded = loadedData.getValue();
            long unloaded = unloadedData.getValue();

            assertThat(loaded).isLessThanOrEqualTo(ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
            assertThat(unloaded).isLessThanOrEqualTo(ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount());
        });
    }
}

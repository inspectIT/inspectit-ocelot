package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EntryPointDetectionTest extends InstrumentationSysTestBase {

    static void methodA(String[][] something, int[] somethingelse) {
        methodB();
    }

    static void methodB() {}

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(EntryPointDetectionTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void verifyEntryPointsDetected() {
        // methodA invokes methodB
        // therefore methodA should be detected as entry point and not method B
        methodA(null, null);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(MetricTestUtils.getDataForView("entrypoint_invocations",
                    ImmutableMap.of("method_name", "methodA")))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );

            assertThat(MetricTestUtils.getDataForView("entrypoint_invocations",
                    ImmutableMap.of("method_name", "methodB")))
                    .isNull();
        });

        // here methodB should be recognized as entry point
        methodB();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(MetricTestUtils.getDataForView("entrypoint_invocations",
                    ImmutableMap.of("method_name", "methodA")))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );

            assertThat(MetricTestUtils.getDataForView("entrypoint_invocations",
                    ImmutableMap.of("method_name", "methodB")))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );
        });
    }
}

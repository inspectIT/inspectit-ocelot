package rocks.inspectit.ocelot.instrumentation;

import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * If the following steps are executed in the right order, the test should succeed: <br>
 * 1. {@code myMethod} should be instrumented by adding a {@code MethodHook} with entry- and exit-actions for invocation <br>
 * 2. When my {@code myMethod} is executed, the invocation should be added to the {@code ConcurrentInvocationManager} <br>
 * 3. While the method is still executing, we wait until the {@code ConcurrentMetricsRecorder} has recorded the invocations
 *    as OpenTelemetry metric <br>
 * 4. We validate the recording via {@code MetricReader} <br>
 * 5. When {@code myMethod} finishes, it should remove the invocation from the {@code ConcurrentInvocationManager} <br>
 * 6. We wait until the {@code ConcurrentMetricsRecorder} has recorded the updated invocations as OpenTelemetry metric <br>
 * 7. We validate the recording via {@code MetricReader} <br>
 */
public class ConcurrentInvocationMetricsSysTest extends InstrumentationSysTestBase {

    void myMethod(Runnable assertions) {
        assertions.run();
    }

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(ConcurrentInvocationMetricsSysTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void shouldRecordInvocationWhenMethodIsCalled() {
        myMethod(() -> assertInvocation(1));
        System.out.println("Invocation ended");

        assertInvocation(0);
    }

    private void assertInvocation(long expected) {
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(MetricTestUtils.getDataForView("inspectit_concurrent_invocations", emptyMap()))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(expected))
        );
    }
}

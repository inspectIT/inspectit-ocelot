package rocks.inspectit.ocelot.instrumentation;

import io.opencensus.stats.*;
import io.opencensus.tags.TagValue;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.metrics.MetricsSysTestBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * If the following steps are executed in the right order, the test should succeed: <br>
 * 1. {@code myMethod} should be instrumented by adding a {@code MethodHook} with entry- and exit-actions for invocation <br>
 * 2. When my {@code myMethod} is executed, the invocation should be added to the {@code ConcurrentInvocationManager} <br>
 * 3. While the method is still executing, we wait until the {@code ConcurrentMetricsRecorder} has recorded the invocations
 *    as OpenCensus measurement <br>
 * 4. We validate the recording via the {@code viewManager} <br>
 * 5. When {@code myMethod} finishes, it should remove the invocation from the {@code ConcurrentInvocationManager} <br>
 * 6. We wait until the {@code ConcurrentMetricsRecorder} has recorded the updated invocations as OpenCensus measurement
 */
public class ConcurrentInvocationMetricsSysTest extends InstrumentationSysTestBase {

    private static final ViewManager viewManager = Stats.getViewManager();

    void myMethod(Runnable assertions) {
        assertions.run();
    }

    @Test
    void shouldRecordInvocationWhenMethodIsCalled() {
        myMethod(() -> assertInvocation(1));

        assertInvocation(0);
    }

    private void assertInvocation(long expected) {
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            ViewData invocationsData = viewManager.getView(View.Name.create("inspectit/concurrent/invocations"));

            assertThat(invocationsData).isNotNull();
            assertThat(invocationsData.getAggregationMap()).isNotEmpty();

            AggregationData.LastValueDataLong invocation = (AggregationData.LastValueDataLong) invocationsData.getAggregationMap()
                    .values().stream().findFirst().get();

            assertThat(invocation.getLastValue()).isEqualTo(expected);
        });
    }
}

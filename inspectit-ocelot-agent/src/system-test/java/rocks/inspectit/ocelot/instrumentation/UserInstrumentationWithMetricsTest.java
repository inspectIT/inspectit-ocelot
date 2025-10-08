package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class UserInstrumentationWithMetricsTest extends InstrumentationSysTestBase {
    
    void invocationCount() {}

    void responseTimeMeasuring() throws Exception {
        Thread.sleep(100);
    }

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(UserInstrumentationWithMetricsTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void invocationHistogramTest() {
        Baggage baggage = Baggage.current().toBuilder().put("user_tag", "user_value").build();
        try (Scope scope = baggage.makeCurrent()) {
            for (int i = 0; i < 7; i++) {
                invocationCount();
            }
        }
        
        String methodName = UserInstrumentationWithMetricsTest.class.getName() + ".invocationCount()";
        Map<String, String> attributes = ImmutableMap.of("user_tag", "user_value", "method_name", methodName);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForHistogramView("my_invocation", attributes))
                        .isNotNull()
                        .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                            assertThat(pointData.getCount()).isEqualTo(7);
                            assertThat(pointData.getSum()).isEqualTo(7 * 42);
                        })
        );
    }

    @Test
    void responseTimeMeasuringTest() throws Exception {
        for (int i = 0; i < 3; i++) {
            responseTimeMeasuring();
        }

        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("service.name", "systemtest"); // set in agent-overwrites.yml
        attributes.put("method", "responseTimeMeasuring()");
        attributes.put("class", UserInstrumentationWithMetricsTest.class.getName());

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForHistogramView("method_duration", attributes))
                        .isNotNull()
                        .isInstanceOfSatisfying(HistogramPointData.class, (pointData) -> {
                            assertThat(pointData.getCount()).isEqualTo(3);
                            assertThat(pointData.getSum()).isBetween(3 * 90.0, 3 * 150.0);
                        })
        );
    }
}

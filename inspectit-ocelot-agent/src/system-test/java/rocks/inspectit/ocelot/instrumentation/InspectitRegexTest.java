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

/**
 * Summary: <br>
 * 1. The {@code instrumentedMethod} will be instrumented and the view is registered in OpenTelemetry. <br>
 * 2. When calling {@code instrumentedMethod}, we will use {@code _regex} and write the result into a metric attribute. <br>
 * 3. Check if the metric was recorded with the attribute.
 */
class InspectitRegexTest extends InstrumentationSysTestBase {

    static final String key = "isMatch";

    void instrumentedMethod() {}

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(InspectitRegexTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void shouldUseInspectitRegex() {
        instrumentedMethod();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView("regexCache",
                        ImmutableMap.of(key, "true")))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                                assertThat(pointData.getValue()).isEqualTo(1)
                        )
        );
    }
}

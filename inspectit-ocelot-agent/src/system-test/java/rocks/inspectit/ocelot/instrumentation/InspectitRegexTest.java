package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricsTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Summary: <br>
 * 1. The {@code instrumentedMethod} will be instrumented and the view is registered in OpenTelemetry. <br>
 * 2. When calling {@code instrumentedMethod}, we will use {@code _regex} and write the result into a metric attribute. <br>
 * 3. Check if the metric was recorded with the attribute.
 */
public class InspectitRegexTest extends InstrumentationSysTestBase {

    private static final String key = "isMatch";

    private void instrumentedMethod() {}

    @Test
    void shouldUseInspectitRegex() {
        instrumentedMethod();

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricsTestUtils.getDataForView("regexCache",
                        ImmutableMap.of(key, "true")))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                                assertThat(pointData.getValue()).isEqualTo(1)
                        )
        );
    }
}

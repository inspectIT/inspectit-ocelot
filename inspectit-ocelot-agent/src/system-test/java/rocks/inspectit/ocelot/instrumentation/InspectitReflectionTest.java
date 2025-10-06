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
 * 1. Only {@code instrumentedMethod} will be instrumented, thus other fields of the class are normally not accessible
 *    in actions. <br>
 * 2. The view is registered in OpenTelemetry. <br>
 * 3. When calling {@code instrumentedMethod}, we will access both {@code hiddenField} and the result of {@code hiddenMethod}
 *    via reflection. <br>
 * 4. The result of the invoked method will be written as metric value. The hidden field will be used as attribute <br>
 */
public class InspectitReflectionTest extends InstrumentationSysTestBase {

    private final String hiddenField = "hidden";

    private int hiddenMethod() {
        return 1;
    }

    private void instrumentedMethod() {}

    @Test
    void shouldAccessHiddenValueAndHiddenMethod() {
        instrumentedMethod();

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricsTestUtils.getDataForView("reflectionInvokedMethod",
                        ImmutableMap.of("field", hiddenField)))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                                assertThat(pointData.getValue()).isEqualTo(1)
                        )
        );
    }
}

package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.AggregationData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Summary: <br>
 * 1. Only {@code instrumentedMethod} will be instrumented, thus other fields of the class are normally not accessible
 *    in actions. <br>
 * 2. The view is registered in OpenCensus. <br>
 * 3. When calling {@code instrumentedMethod}, we will access both {@code hiddenField} and the result of {@code hiddenMethod}
 *    via reflection. <br>
 * 4. The result of the invoked method will be written as metric value. The hidden field will be used as tag <br>
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

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        assertThat(TestUtils.getDataForView("reflectionInvokedMethod",
                ImmutableMap.of("field", hiddenField)))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(1) // same as return value of hiddenMethod()
        );
    }
}

package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.AggregationData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Summary: <br>
 * 1. The {@code instrumentedMethod} will be instrumented and the view is registered in OpenCensus. <br>
 * 2. When calling {@code instrumentedMethod}, we will use {@code _regex} and write the result into a metric tag. <br>
 * 3. Check if the metric was recorded with the tag.
 */
public class InspectitRegexTest extends InstrumentationSysTestBase {

    private static final String key = "isMatch";

    private void instrumentedMethod() {}

    @Test
    void shouldUseInspectitRegex() {
        instrumentedMethod();

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        assertThat(TestUtils.getDataForView("regexCache",
                ImmutableMap.of(key, "true")))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(1)
        );
    }
}

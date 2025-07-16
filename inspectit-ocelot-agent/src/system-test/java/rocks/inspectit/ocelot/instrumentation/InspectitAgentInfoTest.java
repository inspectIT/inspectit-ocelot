package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.AggregationData;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Summary: <br>
 * 1. The {@code instrumentedMethod} will be instrumented and the view is registered in OpenCensus. <br>
 * 2. When calling {@code instrumentedMethod}, we will use {@code _agent} to read the agent information. <br>
 * 3. Check if the metric was recorded with the tags.
 */
public class InspectitAgentInfoTest extends InstrumentationSysTestBase {

    private void instrumentedMethod() {}

    @Test
    void shouldReadAgentInfo() {
        instrumentedMethod();

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        assertThat(TestUtils.getDataForView("agentInfo",
                ImmutableMap.of("agentVersion", "SNAPSHOT", "isSnapshot", "true")))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                        assertThat(c.getCount()).isEqualTo(1)
                );
    }
}

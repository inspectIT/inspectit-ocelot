package rocks.inspectit.oce.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opencensus.common.Scope;
import io.opencensus.stats.AggregationData;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.TestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserInstrumentationWithMetricsTest extends InstrumentationSysTestBase {


    void invocationCount() {

    }

    void responseTimeMeasuring() throws Exception {
        Thread.sleep(100);
    }

    @Test
    void invocationCounterTest() {
        TagContextBuilder builder = Tags.getTagger().currentBuilder()
                .put(TagKey.create("user_tag"), TagValue.create("user_value"));
        try (Scope tcs = builder.buildScoped()) {
            for (int i = 0; i < 7; i++) {
                invocationCount();
            }
        }
        TestUtils.waitForOpenCensusQueueToBeProcessed();

        Map<String, String> countTags = ImmutableMap.of("user_tag", "user_value", "method_name", "invocationCount");
        long invocationCount = ((AggregationData.CountData) TestUtils.getDataForView("my/invocation/count", countTags)).getCount();
        assertThat(invocationCount).isEqualTo(7);

        Map<String, String> sumTags = ImmutableMap.of("method_name", "invocationCount");
        long invocationSum = ((AggregationData.SumDataLong) TestUtils.getDataForView("my/invocation/sum", sumTags)).getSum();
        assertThat(invocationSum).isEqualTo(7 * 42);
    }


    @Test
    void responseTimeMeasuringTest() throws Exception {
        for (int i = 0; i < 3; i++) {
            responseTimeMeasuring();
        }

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        HashMap<String, String> tags = new HashMap<>();
        tags.put("host", ".*");
        tags.put("host-address", ".*");
        tags.put("service-name", ".*");
        tags.put("method_name", "responseTimeMeasuring");

        long count = ((AggregationData.CountData) TestUtils.getDataForView("method/duration/count", tags)).getCount();
        double sum = ((AggregationData.SumDataDouble) TestUtils.getDataForView("method/duration/sum", tags)).getSum();
        assertThat(count).isEqualTo(3);
        assertThat(sum).isBetween(3 * 90.0, 3 * 150.0);
    }
}

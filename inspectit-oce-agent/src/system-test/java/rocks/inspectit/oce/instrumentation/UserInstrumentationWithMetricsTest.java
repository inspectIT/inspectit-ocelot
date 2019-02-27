package rocks.inspectit.oce.instrumentation;

import io.opencensus.common.Scope;
import io.opencensus.stats.AggregationData;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.TestUtils;

import java.util.HashMap;

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

        HashMap<String, String> countTags = new HashMap<>();
        countTags.put("user_tag", "user_value");
        countTags.put("method_name", "invocationCount");
        assertThat(((AggregationData.CountData) TestUtils.getDataForView("my/invocation/count", countTags)).getCount())
                .isEqualTo(7);
        HashMap<String, String> sumTags = new HashMap<>();
        sumTags.put("method_name", "invocationCount");
        assertThat(((AggregationData.SumDataLong) TestUtils.getDataForView("my/invocation/sum", sumTags)).getSum())
                .isEqualTo(7 * 42);

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

        assertThat(((AggregationData.CountData) TestUtils.getDataForView("method/duration/count", tags)).getCount())
                .isEqualTo(3);
        assertThat(((AggregationData.SumDataDouble) TestUtils.getDataForView("method/duration/sum", tags)).getSum())
                .isBetween(3 * 90.0, 3 * 110.0);

    }
}

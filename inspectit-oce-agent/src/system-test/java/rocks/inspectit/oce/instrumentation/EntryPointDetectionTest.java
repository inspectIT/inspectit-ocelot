package rocks.inspectit.oce.instrumentation;

import io.opencensus.stats.AggregationData;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;


public class EntryPointDetectionTest extends InstrumentationSysTestBase {

    static void methodA(String[][] something, int[] somethingelse) {
        methodB();
    }

    static void methodB() {
    }

    @Test
    void verifyEntryPointsDetected() {
        //methodA invokes methodB
        //therefore methodA should be detected as entry point and not method B
        methodA(null, null);

        TestUtils.waitForOpenCensusQueueToBeProcessed();
        assertThat(TestUtils.getDataForView("entrypoint/invocations", Maps.newHashMap("method_name", "methodA")))
                .isInstanceOfSatisfying(AggregationData.CountData.class, data -> assertThat(data.getCount()).isEqualTo(1));
        assertThat(TestUtils.getDataForView("entrypoint/invocations", Maps.newHashMap("method_name", "methodB")))
                .isNull();

        //here methodB should be recognized as entry point
        methodB();

        TestUtils.waitForOpenCensusQueueToBeProcessed();
        assertThat(TestUtils.getDataForView("entrypoint/invocations", Maps.newHashMap("method_name", "methodA")))
                .isInstanceOfSatisfying(AggregationData.CountData.class, data -> assertThat(data.getCount()).isEqualTo(1));
        assertThat(TestUtils.getDataForView("entrypoint/invocations", Maps.newHashMap("method_name", "methodB")))
                .isInstanceOfSatisfying(AggregationData.CountData.class, data -> assertThat(data.getCount()).isEqualTo(1));
    }
}

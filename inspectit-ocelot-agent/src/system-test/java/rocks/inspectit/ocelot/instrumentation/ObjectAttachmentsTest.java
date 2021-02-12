package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.AggregationData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectAttachmentsTest {

    @BeforeAll
    static void waitForInstrumentation() {
        TestUtils.waitForClassInstrumentation(ObjectAttachmentsTest.class, true, 15, TimeUnit.SECONDS);
    }

    public void writeAttachments(String obj, String firstAttachment, String secondAttachment) {

    }

    public void readAttachments(String obj) {

    }

    @Test
    void writeReadTest() {
        String target = "writeReadTest";
        String first = "writeReadTest-f";
        String second = "writeReadTest-s";

        writeAttachments(target, first, second);
        //read twice to make sure that it is not somehow cleared
        readAttachments(target);
        readAttachments(target);

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        assertThat(TestUtils.getDataForView("writeAttachment",
                ImmutableMap.of("target", target)))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(1)
        );

        assertThat(TestUtils.getDataForView("readAttachment",
                ImmutableMap.of("target", target, "firstVal", first, "secondVal", second)))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(2)
        );
    }

    @Test
    void readNullTest() {
        String target = "readNullTest";

        readAttachments(target);

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        assertThat(TestUtils.getDataForView("readAttachment",
                ImmutableMap.of("target", target, "firstVal", ".*", "secondVal", ".*")))
                .isNull();
    }

    @Test
    void replacementTest() {
        String target = "replacementTest";
        String initFirst = "initial-f";
        String initSecond = "initial-s";
        String finalFirst = "final-f";

        writeAttachments(target, initFirst, initSecond);
        writeAttachments(target, finalFirst, null);
        readAttachments(target);

        TestUtils.waitForOpenCensusQueueToBeProcessed();

        assertThat(TestUtils.getDataForView("writeAttachment",
                ImmutableMap.of("target", target)))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(1)
        );

        assertThat(TestUtils.getDataForView("writeAttachment",
                ImmutableMap.of("target", target, "firstVal", initFirst, "secondVal", initSecond)))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(1)
        );

        assertThat(TestUtils.getDataForView("readAttachment",
                ImmutableMap.of("target", target, "firstVal", finalFirst)))
                .isNotNull().isInstanceOfSatisfying(AggregationData.CountData.class, (c) ->
                assertThat(c.getCount()).isEqualTo(1)
        );

        assertThat(TestUtils.getDataForView("readAttachment",
                ImmutableMap.of("target", target, "firstVal", finalFirst, "secondVal", ".*")))
                .isNull();
    }

}

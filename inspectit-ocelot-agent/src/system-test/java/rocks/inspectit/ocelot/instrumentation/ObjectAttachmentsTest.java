package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricsTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ObjectAttachmentsTest extends InstrumentationSysTestBase {

    @BeforeAll
    static void waitForClassInstrumentation() {
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
        // read twice to make sure that it is not somehow cleared
        readAttachments(target);
        readAttachments(target);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(MetricsTestUtils.getDataForView("writeAttachment",
                    ImmutableMap.of("target", target)))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );

            assertThat(MetricsTestUtils.getDataForView("readAttachment",
                    ImmutableMap.of("target", target, "firstVal", first, "secondVal", second)))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(2)
                    );
        });
    }

    @Test
    void readNullTest() {
        String target = "readNullTest";

        readAttachments(target);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricsTestUtils.getDataForView("readAttachment",
                        ImmutableMap.of("target", target, "firstVal", ".*", "secondVal", ".*")))
                        .isNull()
        );
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

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(MetricsTestUtils.getDataForView("writeAttachment",
                    ImmutableMap.of("target", target)))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );

            assertThat(MetricsTestUtils.getDataForView("writeAttachment",
                    ImmutableMap.of("target", target, "firstVal", initFirst, "secondVal", initSecond)))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );

            assertThat(MetricsTestUtils.getDataForView("readAttachment",
                    ImmutableMap.of("target", target, "firstVal", finalFirst)))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(1)
                    );

            assertThat(MetricsTestUtils.getDataForView("readAttachment",
                    ImmutableMap.of("target", target, "firstVal", finalFirst, "secondVal", ".*")))
                    .isNull();
        });
    }
}

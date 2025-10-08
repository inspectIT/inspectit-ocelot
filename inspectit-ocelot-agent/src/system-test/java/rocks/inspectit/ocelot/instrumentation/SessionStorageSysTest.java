package rocks.inspectit.ocelot.instrumentation;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.MetricTestUtils;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Summary: <br>
 * 1. All test methods are instrumented will be instrumented. <br>
 * 2. All views are registered in OpenCensus. <br>
 * 3. When calling {@code firstMethod}, a session-id will be assigned and the data tag will be written and stored for
 *    the session. The {@code firstView} will contain the data tag. <br>
 * 4. When calling {@code secondMethod}, the same session-id will be used to access the data tag, even tough it is not
 *    written by the method call. The {@code secondView} will contain the data tag. <br>
 * 5. When calling {@code thirdMethod} without any assigned session-id, the data tag cannot be used.
 *    Thus, the {@code thirdView} will not contain the data tag.
 */
public class SessionStorageSysTest extends InstrumentationSysTestBase {

    private static final String firstView = "first-session-call";
    private static final String secondView = "second-session-call";
    private static final String thirdView = "third-session-call";

    private static final String dataKey = "session-storage-sys-test-key";
    private static final String dataValue = "session-storage-sys-test-value";

    // Calling with session-id, setting data tag and using data tag
    void firstMethod() {}

    // Calling with session-id and using data tag
    void secondMethod() {}

    // Trying to use data tag without session-id
    void thirdMethod() {}

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(SessionStorageSysTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void shouldAccessDataStoredInSession() {
        firstMethod();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView(firstView,
                        ImmutableMap.of(dataKey, dataValue)))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                                assertThat(pointData.getValue()).isEqualTo(1)
                        )
        );

        secondMethod();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView(secondView,
                        ImmutableMap.of(dataKey, dataValue)))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                                assertThat(pointData.getValue()).isEqualTo(1)
                        )
        );

        thirdMethod();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView(thirdView,
                        ImmutableMap.of(dataKey, dataValue)))
                        .isNull()
        );
    }
}

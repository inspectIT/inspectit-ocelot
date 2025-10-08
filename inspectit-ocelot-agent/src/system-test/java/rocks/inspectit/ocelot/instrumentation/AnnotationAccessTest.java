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

class AnnotationAccessTest extends InstrumentationSysTestBase {

    @MyMethodAnnotation("helloworld")
    void myAnnotatedMethod(String someArgument) {}

    private static class ConstructorAnnotated {

        @MyMethodAnnotation("helloconstructor")
        private ConstructorAnnotated() {}

        public static ConstructorAnnotated createInstance() {
            return new ConstructorAnnotated();
        }
    }

    static {
        // force the initialization of the class
        AnnotationAccessTest.class.getDeclaredMethods();
        ConstructorAnnotated.class.getDeclaredMethods();
    }

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(AnnotationAccessTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void verifyAnnotationValuesExtractedFromMethod() {
        myAnnotatedMethod("blub");

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(MetricTestUtils.getDataForView("annotation_test",
                    ImmutableMap.of("anno_value", "helloworld")))
                    .isNotNull()
                    .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                            assertThat(pointData.getValue()).isEqualTo(42)
                    )
        );
    }

    @Test
    void verifyAnnotationValuesExtractedFromConstructor() {
        ConstructorAnnotated.createInstance();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(MetricTestUtils.getDataForView("annotation_test",
                        ImmutableMap.of("anno_value", "helloconstructor")))
                        .isNotNull()
                        .isInstanceOfSatisfying(LongPointData.class, (pointData) ->
                                assertThat(pointData.getValue()).isEqualTo(42)
                        )
        );
    }
}

package rocks.inspectit.oce.instrumentation;

import io.opencensus.stats.AggregationData;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import rocks.inspectit.oce.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationAccessTest extends InstrumentationSysTestBase {


    @MyMethodAnnotation("helloworld")
    void myAnnotatedMethod(String someArgument) {

    }

    private static class ConstructorAnnotated {

        @MyMethodAnnotation("helloconstructor")
        private ConstructorAnnotated() {
        }

        public static ConstructorAnnotated createInstance() {
            return new ConstructorAnnotated();
        }
    }

    static {
        //force the initialization of the class
        AnnotationAccessTest.class.getDeclaredMethods();
        ConstructorAnnotated.class.getDeclaredMethods();
    }

    @Test
    void verifyAnnotationValuesExtractedFromMethod() {
        myAnnotatedMethod("blub");
        TestUtils.waitForOpenCensusQueueToBeProcessed();

        AggregationData data = TestUtils.getDataForView("annotation/test", Maps.newHashMap("anno_value", "helloworld"));

        assertThat(((AggregationData.LastValueDataLong) data).getLastValue())
                .isEqualTo(42);
    }

    @Test
    void verifyAnnotationValuesExtractedFromConstructor() {
        ConstructorAnnotated.createInstance();
        TestUtils.waitForOpenCensusQueueToBeProcessed();

        AggregationData data = TestUtils.getDataForView("annotation/test", Maps.newHashMap("anno_value", "helloconstructor"));
        assertThat(((AggregationData.LastValueDataLong) data).getLastValue())
                .isEqualTo(42);
    }

}


package rocks.inspectit.ocelot.instrumentation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericActionsTest extends InstrumentationSysTestBase {

    void argumentAccessTest(NamedElement elem, Runnable assertions) {
        assertions.run();
    }

    @BeforeAll
    static void waitForClassInstrumentation() {
        TestUtils.waitForClassInstrumentation(GenericActionsTest.class, true, 30, TimeUnit.SECONDS);
    }

    @Test
    void verifyArgumentsAccessible() {
        argumentAccessTest(() -> "test123", () -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsEntry("name_via_arg0", "test123");
            assertThat(attributes).containsEntry("name_via_args", "test123");
            assertThat(attributes).containsEntry("name_reversed", "321tset");
            assertThat(attributes).containsEntry("name_reversed_upper", "321TSET");
        });
    }

    void constantParsingTest(Duration dur, Runnable assertions) {
        assertions.run();
    }

    @Test
    void verifyConstantArgumentsParsedCorrectly() {
        constantParsingTest(Duration.ofMillis(1500), () -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsEntry("result", "3500");
        });
    }

    @Test
    void testDefaultMethodInstrumented() {
        NamedElement n1 = () -> "blablub";
        n1.doSomething(() -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsEntry("name", "blablub");
        });

        NamedElement n2 = new NamedElement() {
            private String name = "something";

            @Override
            public void doSomething(Runnable r) {
                name = "somethingelse";
                r.run();
            }

            @Override
            public String getName() {
                return name;
            }
        };
        //call the anonymous classes to make sure they are loaded
        n1.getName();
        n2.getName();
        waitForInstrumentation(); //wait because until here the class has most likely not been loaded yet
        n1.doSomething(() -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsEntry("name", "blablub");
        });
        n2.doSomething(() -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsEntry("name", "something");
        });
        n2.doSomething(() -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsEntry("name", "somethingelse");
        });
    }


    void conditionsTest(Runnable assertions) {
        assertions.run();
    }

    @Test
    void verifyConditionsBehaveAsExpected() {
        conditionsTest(() -> {
            Map<String, String> attributes = TestUtils.getCurrentAttributesAsMap();
            assertThat(attributes).containsKey("only_if_true_executed");
            assertThat(attributes).doesNotContainKey("only_if_true_skipped");
            assertThat(attributes).doesNotContainKey("only_if_true_also_skipped");
            assertThat(attributes).containsKey("only_if_false_executed");
            assertThat(attributes).doesNotContainKey("only_if_false_skipped");
            assertThat(attributes).doesNotContainKey("only_if_false_also_skipped");
            assertThat(attributes).containsKey("only_if_null_executed");
            assertThat(attributes).doesNotContainKey("only_if_null_skipped");
            assertThat(attributes).containsKey("only_if_not_null_executed");
            assertThat(attributes).doesNotContainKey("only_if_not_null_skipped");
        });
    }

}


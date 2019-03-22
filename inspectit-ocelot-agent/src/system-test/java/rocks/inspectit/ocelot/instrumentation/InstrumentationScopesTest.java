package rocks.inspectit.ocelot.instrumentation;

import org.junit.jupiter.api.Test;
import rocks.inspectit.ocelot.utils.TestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class InstrumentationScopesTest extends InstrumentationSysTestBase {


    @Test
    void testNonOverriddenDefaultMethodInstrumentedForAnonymousClass() {
        NamedElement n1 = new NamedElement() {
            @Override
            public String getName() {
                return "blablub";
            }
        };
        waitForInstrumentation(); //wait because until here the class has most likely not been loaded yet
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "blablub");
        });
    }

    @Test
    void testNonOverriddenDefaultMethodInstrumentedForLambda() {
        NamedElement n1 = () -> "i'm a lambda";
        waitForInstrumentation(); //wait because until here the class has most likely not been loaded yet
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "i'm a lambda");
        });
    }

    @Test
    void testOverriddenDefaultMethodInstrumentedForAnonymousClass() {
        NamedElement n1 = new NamedElement() {
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
        waitForInstrumentation();
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "something");
        });
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "somethingelse");
        });
    }

    @FunctionalInterface
    private interface AdaptedNamedElement extends NamedElement {

        @Override
        default String getName() {
            return "AdaptedNamedElement";
        }

        @Override
        void doSomething(Runnable r);
    }


    //instrumenting lambdas containing default methods is not supported yet
    //@Test
    void testOverriddenDefaultMethodInstrumentedForLambda() {
        AdaptedNamedElement n1 = (r) -> r.run();
        waitForInstrumentation(); //wait because until here the class has most likely not been loaded yet
        n1.doSomething(() -> {
            Map<String, String> tags = TestUtils.getCurrentTagsAsMap();
            assertThat(tags).containsEntry("name", "AdaptedNamedElement");
        });
    }
}

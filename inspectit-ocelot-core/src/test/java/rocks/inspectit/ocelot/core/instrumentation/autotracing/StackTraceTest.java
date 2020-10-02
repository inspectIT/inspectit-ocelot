package rocks.inspectit.ocelot.core.instrumentation.autotracing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StackTraceTest {

    @Nested
    static class Cleanup {

        @Test
        void verifyStackTopCleaned() {
            StackTraceElement rootFrame = new StackTraceElement("Root", "rootMethod", "", -1);
            StackTraceElement calleeFrame = new StackTraceElement("Callee", "callee", "", -1);
            StackTraceElement[] input = {
                    new StackTraceElement("rocks.inspectit.ocelot.bootstrap.Instances", "getSome", "", -1),
                    new StackTraceElement("rocks.inspectit.ocelot.core.SomeClass", "doStuff", "", -1),
                    calleeFrame,
                    rootFrame,
            };

            StackTrace st = new StackTrace(input);

            assertThat(st.size()).isEqualTo(2);
            assertThat(st.get(0)).isSameAs(rootFrame);
            assertThat(st.get(1)).isSameAs(calleeFrame);
        }

        @Test
        void verifyLambdaRemoved() {
            Runnable r1 = () -> {
            };
            StackTraceElement rootFrame = new StackTraceElement("Root", "rootMethod", "", -1);
            StackTraceElement calleeFrame = new StackTraceElement("Callee", "callee", "", -1);
            StackTraceElement[] input = {
                    new StackTraceElement("rocks.inspectit.ocelot.bootstrap.Instances", "getSome", "", -1),
                    calleeFrame,
                    new StackTraceElement("not." + r1.getClass().getName(), "run", "", -1),
                    rootFrame,
            };

            StackTrace st = new StackTrace(input);

            assertThat(st.size()).isEqualTo(2);
            assertThat(st.get(0)).isSameAs(rootFrame);
            assertThat(st.get(1)).isSameAs(calleeFrame);
        }

    }
}
